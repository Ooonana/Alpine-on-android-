# Alpine Auto-Launch (v66)
PREFIX="/data/data/com.alpine/files/usr"
ROOTFS="$PREFIX/var/lib/proot-distro/installed-rootfs/alpine"
REPOSITORIES="$ROOTFS/etc/apk/repositories"
REMOTE_REPOSITORIES="$ROOTFS/etc/apk/remote-repositories"
RESOLV_CONF="$ROOTFS/etc/resolv.conf"
PROOT_WORK_DIR="$PREFIX/tmp/proot"

export HOME="/data/data/com.alpine/files/home"
export TMPDIR="$PREFIX/tmp"

# Keep host Termux injection out of proot/proot-distro.
unset LD_PRELOAD

export PROOT_NO_SECCOMP=1
export PROOT_FORCE_NO_SECCOMP=1
export PROOT_ASSUME_MEMFD_UNSUPPORTED=1
export PROOT_KILL_ON_EXIT=1
export PROOT_TMP_DIR="$PROOT_WORK_DIR"
export LD_LIBRARY_PATH="$PREFIX/lib"
export PATH="$PREFIX/bin:$PATH"
export MAGIC="$PREFIX/share/file/magic.mgc"

ensure_alpine_runtime() {
    mkdir -p "$TMPDIR" "$PROOT_WORK_DIR" "$ROOTFS/etc" "$ROOTFS/etc/apk" \
        "$ROOTFS/run/dbus" "$ROOTFS/var/empty" "$ROOTFS/var/run/pulse" \
        "$ROOTFS/var/cache/apk-mirror/main/aarch64" \
        "$ROOTFS/var/cache/apk-mirror/community/aarch64" "$ROOTFS/usr/local/sbin" \
        "$ROOTFS/usr/local/bin" 2>/dev/null || true
    chmod 700 "$PROOT_WORK_DIR" 2>/dev/null || true
    chmod 1777 "$TMPDIR" "$ROOTFS/tmp" 2>/dev/null || true
    chmod 755 "$ROOTFS/run" "$ROOTFS/run/dbus" "$ROOTFS/var/run" 2>/dev/null || true
    touch "$ROOTFS/etc/environment" 2>/dev/null || true
    chmod 600 "$ROOTFS/etc/environment" 2>/dev/null || true
    chmod 700 "$ROOTFS/sbin/apk.static" "$ROOTFS/usr/local/sbin/apk" 2>/dev/null || true
    rm -f "$ROOTFS/run/dbus/pid" 2>/dev/null || true

    if [ ! -s "$RESOLV_CONF" ]; then
        {
            echo "nameserver 8.8.8.8"
            echo "nameserver 8.8.4.4"
        } > "$RESOLV_CONF" 2>/dev/null || true
    fi

    if [ ! -s "$REMOTE_REPOSITORIES" ]; then
        {
            echo "https://dl-cdn.alpinelinux.org/alpine/v3.24/main"
            echo "https://dl-cdn.alpinelinux.org/alpine/v3.24/community"
        } > "$REMOTE_REPOSITORIES" 2>/dev/null || true
    fi

    if ! grep -q '^/var/cache/apk-mirror/' "$REPOSITORIES" 2>/dev/null; then
        {
            echo "/var/cache/apk-mirror/main"
            echo "/var/cache/apk-mirror/community"
        } > "$REPOSITORIES" 2>/dev/null || true
    fi
}

start_x11_bridge() {
    bridge_pid_file="$TMPDIR/alpine-x11-bridge.pid"
    request_file="$TMPDIR/alpine-x11-request"
    log_file="$TMPDIR/termux-x11.log"

    if [ -r "$bridge_pid_file" ]; then
        bridge_pid="$(cat "$bridge_pid_file" 2>/dev/null)"
        if [ -n "$bridge_pid" ] && kill -0 "$bridge_pid" 2>/dev/null; then
            return 0
        fi
    fi

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
                socket_path="$TMPDIR/.X11-unix/X$display_number"

                : > "$log_file"
                echo "Host X11 bridge starting DISPLAY=$display" >> "$log_file"
                echo "Host cwd: $(pwd 2>/dev/null || echo unknown)" >> "$log_file"

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
	                    echo "$!" > "$TMPDIR/alpine-x11-server.pid" 2>/dev/null || true
	                fi
            fi
            sleep 1
        done
    ) >/dev/null 2>&1 &

    echo "$!" > "$bridge_pid_file" 2>/dev/null || true
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
        --env XDG_RUNTIME_DIR=/tmp \
        "$@"

    if [ "${ALPINE_DISABLE_SYSVIPC:-0}" = "1" ]; then
        set -- --no-sysvipc "$@"
    fi
    if [ "${ALPINE_DISABLE_LINK2SYMLINK:-0}" = "1" ]; then
        set -- --no-link2symlink "$@"
    fi

    "$PREFIX/bin/proot-distro" login alpine "$@"
}

run_alpine_direct_proot() {
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
        -b /sdcard \
        -w /root \
        /usr/bin/env -i \
        HOME=/root \
        TERM="${TERM:-xterm-256color}" \
        DISPLAY="${DISPLAY:-:1}" \
        TMPDIR=/tmp \
        XDG_RUNTIME_DIR=/tmp \
        PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
        IN_ALPINE=1 \
        "$@"
}

if [ -z "$IN_ALPINE" ] && [ "$ALPINE_FAILSAFE" != "1" ]; then
    export DISPLAY="${DISPLAY:-:1}"
    ensure_alpine_runtime
    start_x11_bridge

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
