# Alpine Auto-Launch (v68)
PREFIX="/data/data/com.alpine/files/usr"
ROOTFS="$PREFIX/var/lib/proot-distro/installed-rootfs/alpine"
REPOSITORIES="$ROOTFS/etc/apk/repositories"
RESOLV_CONF="$ROOTFS/etc/resolv.conf"
PROOT_WORK_DIR="$PREFIX/tmp/proot"

export HOME="/data/data/com.alpine/files/home"
export TMPDIR="$PREFIX/tmp"

# Keep host Termux injection out of proot/proot-distro.
unset LD_PRELOAD

# Modern Termux PRoot enables its seccomp accelerator by default. Do not
# disable it globally: current PRoot contains Android/SIGSYS handling needed
# for modern kernels. A device-specific workaround can still be requested by
# launching the app/session with PROOT_NO_SECCOMP=1.
export PROOT_ASSUME_MEMFD_UNSUPPORTED=1
export PROOT_TMP_DIR="$PROOT_WORK_DIR"
export LD_LIBRARY_PATH="$PREFIX/lib"
export PATH="$PREFIX/bin:$PATH"
export MAGIC="$PREFIX/share/file/magic.mgc"

ensure_alpine_runtime() {
    mkdir -p "$TMPDIR" "$TMPDIR/alpine-runtime-0" "$PROOT_WORK_DIR" "$ROOTFS/etc" "$ROOTFS/etc/apk" \
        "$ROOTFS/run/dbus" "$ROOTFS/var/empty" "$ROOTFS/var/run/pulse" \
        "$ROOTFS/tmp/alpine-runtime-0" "$ROOTFS/usr/local/bin" 2>/dev/null || true
    chmod 700 "$PROOT_WORK_DIR" 2>/dev/null || true
    chmod 1777 "$TMPDIR" "$ROOTFS/tmp" 2>/dev/null || true
    chmod 0700 "$TMPDIR/alpine-runtime-0" "$ROOTFS/tmp/alpine-runtime-0" 2>/dev/null || true
    chmod 755 "$ROOTFS/run" "$ROOTFS/run/dbus" "$ROOTFS/var/run" 2>/dev/null || true
    touch "$ROOTFS/etc/environment" 2>/dev/null || true
    chmod 600 "$ROOTFS/etc/environment" 2>/dev/null || true
    rm -f "$ROOTFS/run/dbus/pid" 2>/dev/null || true

    # Seed DNS on first launch, or refresh it only when explicitly requested.
    # Preserve a user's non-empty resolv.conf during normal subsequent launches.
    if [ ! -s "$RESOLV_CONF" ] || [ -n "${ALPINE_DNS_SERVERS:-}" ] || [ "${ALPINE_DNS_FORCE:-0}" = "1" ]; then
        resolver_tmp="$TMPDIR/alpine-resolv.conf.${BASHPID:-$PPID}"
        : > "$resolver_tmp" 2>/dev/null || resolver_tmp=""
        if [ -n "$resolver_tmp" ]; then
            if [ -n "${ALPINE_DNS_SERVERS:-}" ]; then
                dns_candidates="$ALPINE_DNS_SERVERS"
            elif [ -x /system/bin/getprop ]; then
                dns_candidates="$(
                    /system/bin/getprop 2>/dev/null |
                        sed -n 's/^\[[^]]*\.dns[1-4]\]: \[\([^]]*\)\]$/\1/p' |
                        tr '\n' ' '
                )"
            else
                dns_candidates=""
            fi

            for dns in $dns_candidates; do
                case "$dns" in
                    ""|*[!0-9A-Fa-f:.]*) continue ;;
                esac
                if ! grep -qxF "nameserver $dns" "$resolver_tmp" 2>/dev/null; then
                    echo "nameserver $dns" >> "$resolver_tmp"
                fi
            done

            if [ -s "$resolver_tmp" ]; then
                cat "$resolver_tmp" > "$RESOLV_CONF" 2>/dev/null || true
            elif [ ! -s "$RESOLV_CONF" ]; then
                {
                    echo "nameserver 8.8.8.8"
                    echo "nameserver 8.8.4.4"
                } > "$RESOLV_CONF" 2>/dev/null || true
            fi
            rm -f "$resolver_tmp" 2>/dev/null || true
        fi
    fi

    # V68 uses Alpine's stock apk and normal remote repositories. Only repair
    # an empty repository file; do not overwrite a user's chosen mirror.
    if [ ! -s "$REPOSITORIES" ]; then
        {
            echo "https://dl-cdn.alpinelinux.org/alpine/v3.23/main"
            echo "https://dl-cdn.alpinelinux.org/alpine/v3.23/community"
        } > "$REPOSITORIES" 2>/dev/null || true
    fi
}

proc_start_time() {
    pid="$1"
    case "$pid" in
        ""|*[!0-9]*) return 1 ;;
    esac
    [ -r "/proc/$pid/stat" ] || return 1
    stat_line="$(cat "/proc/$pid/stat" 2>/dev/null)" || return 1
    stat_fields="${stat_line#*) }"
    [ "$stat_fields" != "$stat_line" ] || return 1
    stat_start="$(printf '%s\n' "$stat_fields" | awk 'NF >= 20 { print $20; found=1 } END { if (!found) exit 1 }')" || return 1
    [ -n "$stat_start" ] || return 1
    printf '%s\n' "$stat_start"
}

pid_identity_file_alive() {
    identity_file="$1"
    [ -r "$identity_file" ] || return 1
    read -r identity_pid identity_start < "$identity_file" || return 1
    [ -n "$identity_start" ] || return 1
    kill -0 "$identity_pid" 2>/dev/null || return 1
    current_start="$(proc_start_time "$identity_pid")" || return 1
    [ "$current_start" = "$identity_start" ]
}

write_pid_identity() {
    identity_file="$1"
    identity_pid="$2"
    identity_start="$(proc_start_time "$identity_pid")" || return 1
    printf '%s %s\n' "$identity_pid" "$identity_start" > "$identity_file"
}

start_x11_bridge() {
    bridge_pid_file="$TMPDIR/alpine-x11-bridge.pid"
    server_pid_file="$TMPDIR/alpine-x11-server.pid"
    request_file="$TMPDIR/alpine-x11-request"
    log_file="$TMPDIR/termux-x11.log"

    if pid_identity_file_alive "$bridge_pid_file"; then
        return 0
    fi
    rm -f "$bridge_pid_file" 2>/dev/null || true

    (
        export PREFIX HOME TMPDIR
        export PATH="/system/bin:/system/xbin:$PREFIX/bin:$PATH"
        cd "$TMPDIR" 2>/dev/null || cd "$PREFIX/tmp" 2>/dev/null || cd / 2>/dev/null || exit 0

        while :; do
            if [ -s "$request_file" ]; then
                display="$(sed -n '1p' "$request_file" 2>/dev/null)"
                rm -f "$request_file"

                [ -n "$display" ] || display=":1"
                case "$display" in
                    :*) ;;
                    *) display=":$display" ;;
                esac

                display_number="${display#:}"
                display_number="${display_number%%.*}"
                case "$display_number" in
                    ""|*[!0-9]*)
                        echo "Ignoring invalid X11 display request: $display" >> "$log_file"
                        continue
                        ;;
                esac
                socket_path="$TMPDIR/.X11-unix/X$display_number"

                : > "$log_file"
                echo "Host X11 bridge starting DISPLAY=$display" >> "$log_file"
                echo "Host cwd: $(pwd 2>/dev/null || echo unknown)" >> "$log_file"

                if [ -S "$socket_path" ] && ! pid_identity_file_alive "$server_pid_file"; then
                    echo "Removing stale X11 socket at $socket_path" >> "$log_file"
                    rm -f "$socket_path" "$server_pid_file" 2>/dev/null || true
                fi

                if [ -S "$socket_path" ]; then
                    echo "X11 socket already exists at $socket_path" >> "$log_file"
	                else
	                    mkdir -p "$TMPDIR/.X11-unix" 2>/dev/null || true
	                    export XKB_CONFIG_ROOT="$ROOTFS/usr/share/X11/xkb"
	                    echo "XKB_CONFIG_ROOT=$XKB_CONFIG_ROOT" >> "$log_file"
	                    if [ -d "$ROOTFS/usr/share/fonts/encodings" ]; then
	                        export FONT_ENCODINGS_DIRECTORY="$ROOTFS/usr/share/fonts/encodings"
	                        echo "FONT_ENCODINGS_DIRECTORY=$FONT_ENCODINGS_DIRECTORY" >> "$log_file"
	                    fi

	                    font_path=""
	                    for font_dir in \
	                        "$ROOTFS/usr/share/fonts/misc" \
	                        "$ROOTFS/usr/share/fonts/100dpi" \
	                        "$ROOTFS/usr/share/fonts/75dpi" \
	                        "$ROOTFS/usr/share/fonts/Type1" \
	                        "$ROOTFS/usr/share/fonts/TTF" \
	                        "$ROOTFS/usr/share/fonts/OTF" \
	                        "$ROOTFS/usr/share/fonts/dejavu"; do
	                        if [ -d "$font_dir" ]; then
	                            if [ -z "$font_path" ]; then
	                                font_path="$font_dir"
	                            else
	                                font_path="$font_path,$font_dir"
	                            fi
	                        fi
	                    done

	                    if [ -n "$font_path" ]; then
	                        echo "X11_FONT_PATH=$font_path" >> "$log_file"
	                    fi
	                    echo "Open the embedded display Activity from Alpine app UI." >> "$log_file"

	                    if [ -n "$font_path" ]; then
	                        "$PREFIX/bin/termux-x11" "$display" -fp "$font_path" >>"$log_file" 2>&1 &
	                    else
	                        "$PREFIX/bin/termux-x11" "$display" >>"$log_file" 2>&1 &
	                    fi
	                    server_pid="$!"
	                    if ! write_pid_identity "$server_pid_file" "$server_pid"; then
	                        echo "WARNING: could not record X11 server identity" >> "$log_file"
	                    fi
	                fi
            fi
            sleep 1
        done
    ) >/dev/null 2>&1 &

    bridge_pid="$!"
    if ! write_pid_identity "$bridge_pid_file" "$bridge_pid"; then
        kill "$bridge_pid" 2>/dev/null || true
        echo "WARNING: could not record Alpine X11 bridge identity" >&2
        return 1
    fi
}

detect_android_sdk() {
    local sdk="${ALPINE_ANDROID_SDK:-}"
    case "$sdk" in
        ""|*[!0-9]*) sdk="" ;;
        *) printf '%s\n' "$sdk"; return 0 ;;
    esac

    if [ -x "$PREFIX/bin/getprop" ]; then
        sdk="$("$PREFIX/bin/getprop" ro.build.version.sdk 2>/dev/null || true)"
    elif [ -x /system/bin/getprop ]; then
        sdk="$(/system/bin/getprop ro.build.version.sdk 2>/dev/null || true)"
    fi

    case "$sdk" in
        ""|*[!0-9]*) return 1 ;;
        *) printf '%s\n' "$sdk" ;;
    esac
}

run_alpine_proot_distro() {
    # Keep proot-distro's Android defaults for hard-link and SysV IPC
    # emulation. The opt-out variables below are diagnostic escape hatches
    # for device-specific kernel/SELinux failures, not normal launch flags.
    set -- \
        --shared-tmp \
        --work-dir /root \
        --env DISPLAY="${DISPLAY:-:1}" \
        --env TMPDIR=/tmp \
        --env XDG_RUNTIME_DIR=/tmp/alpine-runtime-0 \
        "$@"

    if [ -n "${ALPINE_ANDROID_SDK:-}" ]; then
        set -- --env ALPINE_ANDROID_SDK="$ALPINE_ANDROID_SDK" "$@"
    fi

    if [ "${ALPINE_DISABLE_SYSVIPC:-0}" = "1" ]; then
        set -- --no-sysvipc "$@"
    fi
    if [ "${ALPINE_DISABLE_LINK2SYMLINK:-0}" = "1" ]; then
        set -- --no-link2symlink "$@"
    fi

    "$PREFIX/bin/proot-distro" login alpine "$@"
}

run_alpine_direct_proot() {
    local -a optional_binds=()
    if [ -d /sdcard ] && [ -r /sdcard ]; then
        optional_binds+=(-b /sdcard)
    fi
    local fd fd_name
    local -a fd_names=(stdin stdout stderr)
    for fd in 0 1 2; do
        if [ -e "/proc/self/fd/$fd" ]; then
            fd_name="${fd_names[$fd]}"
            optional_binds+=(-b "/proc/self/fd/$fd:/dev/$fd_name")
        fi
    done
    if [ -d "$TMPDIR" ]; then
        optional_binds+=(-b "$TMPDIR:/dev/shm")
    fi

    "$PREFIX/bin/proot" \
        --kill-on-exit \
        -0 \
        -r "$ROOTFS" \
        -b /dev \
        -b /proc \
        -b /sys \
        -b /dev/urandom:/dev/random \
        -b /proc/self/fd:/dev/fd \
        -b "$TMPDIR:/tmp" \
        -b "$HOME:/root" \
        "${optional_binds[@]}" \
        -w /root \
        /usr/bin/env -i \
        HOME=/root \
        TERM="${TERM:-xterm-256color}" \
        DISPLAY="${DISPLAY:-:1}" \
        TMPDIR=/tmp \
        XDG_RUNTIME_DIR=/tmp/alpine-runtime-0 \
        ALPINE_ANDROID_SDK="${ALPINE_ANDROID_SDK:-}" \
        PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
        IN_ALPINE=1 \
        "$@"
}

if [ -z "$IN_ALPINE" ] && [ "$ALPINE_FAILSAFE" != "1" ]; then
    export DISPLAY="${DISPLAY:-:1}"
    ensure_alpine_runtime
    ALPINE_ANDROID_SDK="$(detect_android_sdk 2>/dev/null || true)"
    export ALPINE_ANDROID_SDK
    if ! start_x11_bridge; then
        echo "WARNING: Alpine display bridge could not start; terminal mode is still available."
    fi

    if [ ! -x "$PREFIX/bin/proot-distro" ]; then
        echo "Alpine could not start: proot-distro is missing."
        echo "Recovery shell is active."
        export ALPINE_FAILSAFE=1
        return 0
    fi

    if [ ! -x "$PREFIX/bin/proot" ]; then
        echo "Alpine could not start: proot is missing."
        echo "Recovery shell is active."
        export ALPINE_FAILSAFE=1
        return 0
    fi

    if [ ! -x "$ROOTFS/bin/sh" ]; then
        echo "Alpine could not start: the root filesystem is incomplete."
        echo "Recovery shell is active."
        export ALPINE_FAILSAFE=1
        return 0
    fi

    launch_log="$TMPDIR/alpine-launch.log"
    : > "$launch_log" 2>/dev/null || true

    # Probe startup separately from the interactive shell. A user's later
    # non-zero `exit` must not be mistaken for a launch failure and trigger a
    # second fallback shell.
    if run_alpine_proot_distro -- /bin/sh -c 'exit 0' >>"$launch_log" 2>&1; then
        run_alpine_proot_distro
        status=$?
        if [ "${ALPINE_LAUNCH_DEBUG:-0}" = "1" ] && [ "$status" -ne 0 ]; then
            echo "Alpine session ended with status $status."
        fi
    else
        echo "Standard Alpine launch is unavailable; using compatibility mode."
        if run_alpine_direct_proot /bin/sh -c 'exit 0' >>"$launch_log" 2>&1; then
            run_alpine_direct_proot /bin/sh -l
            status=$?
            if [ "${ALPINE_LAUNCH_DEBUG:-0}" = "1" ] && [ "$status" -ne 0 ]; then
                echo "Compatibility session ended with status $status."
            fi
        else
            echo "Alpine could not start. Recovery shell is active."
            echo "Diagnostics: $launch_log"
            export ALPINE_FAILSAFE=1
        fi
    fi
fi
