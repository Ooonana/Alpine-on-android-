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

ensure_group_entry() {
    name="$1"
    gid="$2"
    file="$ROOTFS/etc/group"

    grep -q "^${name}:" "$file" 2>/dev/null && return 0
    echo "${name}:x:${gid}:" >> "$file" 2>/dev/null || true
}

ensure_passwd_entry() {
    name="$1"
    uid="$2"
    gid="$3"
    gecos="$4"
    home="$5"
    shell="$6"
    passwd_file="$ROOTFS/etc/passwd"
    shadow_file="$ROOTFS/etc/shadow"

    grep -q "^${name}:" "$passwd_file" 2>/dev/null ||
        echo "${name}:x:${uid}:${gid}:${gecos}:${home}:${shell}" >> "$passwd_file" 2>/dev/null || true

    if [ -f "$shadow_file" ]; then
        grep -q "^${name}:" "$shadow_file" 2>/dev/null ||
            echo "${name}:*:19000:0:99999:7:::" >> "$shadow_file" 2>/dev/null || true
    fi
}

ensure_alpine_runtime() {
    mkdir -p "$TMPDIR" "$PROOT_WORK_DIR" "$ROOTFS/etc" "$ROOTFS/etc/apk" \
        "$ROOTFS/run/dbus" "$ROOTFS/var/empty" "$ROOTFS/var/run/pulse" \
        "$ROOTFS/var/cache/apk-mirror/main/aarch64" \
        "$ROOTFS/var/cache/apk-mirror/community/aarch64" "$ROOTFS/usr/local/sbin" \
        "$ROOTFS/usr/local/bin" 2>/dev/null || true
    chmod 700 "$PROOT_WORK_DIR" 2>/dev/null || true
    chmod 1777 "$TMPDIR" "$ROOTFS/tmp" 2>/dev/null || true
    chmod 755 "$ROOTFS/run" "$ROOTFS/run/dbus" "$ROOTFS/var/run" 2>/dev/null || true
    touch "$ROOTFS/etc/environment" "$ROOTFS/etc/passwd" "$ROOTFS/etc/group" 2>/dev/null || true
    chmod 600 "$ROOTFS/etc/environment" 2>/dev/null || true
    chmod 700 "$ROOTFS/sbin/apk.static" "$ROOTFS/usr/local/sbin/apk" 2>/dev/null || true
    rm -f "$ROOTFS/run/dbus/pid" "$ROOTFS/var/run/dbus/pid" 2>/dev/null || true

    if [ ! -s "$RESOLV_CONF" ]; then
        {
            echo "nameserver 8.8.8.8"
            echo "nameserver 8.8.4.4"
        } > "$RESOLV_CONF" 2>/dev/null || true
    fi

    if [ ! -s "$REMOTE_REPOSITORIES" ]; then
        {
            echo "http://dl-cdn.alpinelinux.org/alpine/v3.23/main"
            echo "http://dl-cdn.alpinelinux.org/alpine/v3.23/community"
        } > "$REMOTE_REPOSITORIES" 2>/dev/null || true
    fi

    if ! grep -q '^/var/cache/apk-mirror/' "$REPOSITORIES" 2>/dev/null; then
        {
            echo "/var/cache/apk-mirror/main"
            echo "/var/cache/apk-mirror/community"
        } > "$REPOSITORIES" 2>/dev/null || true
    fi

    {
        echo "passwd: files"
        echo "group: files"
        echo "shadow: files"
        echo "hosts: files dns"
    } > "$ROOTFS/etc/nsswitch.conf" 2>/dev/null || true

    ensure_group_entry messagebus 81
    ensure_group_entry polkitd 102
    ensure_group_entry pulse 103
    ensure_passwd_entry messagebus 81 81 messagebus /run/dbus /sbin/nologin
    ensure_passwd_entry polkitd 102 102 polkitd /var/empty /sbin/nologin
    ensure_passwd_entry pulse 103 103 pulse /var/run/pulse /sbin/nologin
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

if [ -z "$IN_ALPINE" ] && [ "$ALPINE_FAILSAFE" != "1" ]; then
    export DISPLAY="${DISPLAY:-:1}"
    ensure_alpine_runtime
    start_x11_bridge
    echo "--- Alpine v66 launcher ---"
    echo "RootFS: $ROOTFS"

    if [ ! -x "$PREFIX/bin/proot-distro" ]; then
        echo "Error: proot-distro missing at $PREFIX/bin/proot-distro"
        export ALPINE_FAILSAFE=1
        return 0
    fi

    if [ ! -x "$ROOTFS/bin/sh" ]; then
        echo "Error: Alpine shell missing or not executable at $ROOTFS/bin/sh"
        export ALPINE_FAILSAFE=1
        return 0
    fi

    echo "Launching Alpine via proot-distro..."
    "$PREFIX/bin/proot-distro" login alpine \
        --shared-tmp \
        --no-link2symlink \
        --no-sysvipc \
        --work-dir /root \
        --env DISPLAY="${DISPLAY:-:1}" \
        --env TMPDIR=/tmp \
        --env XDG_RUNTIME_DIR=/tmp
    status=$?

    if [ "$status" -ne 0 ]; then
        echo "proot-distro failed with status $status. Attempting direct proot fallback..."
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
            /bin/sh -l
        status=$?
    fi

    if [ "$status" -ne 0 ]; then
        echo "CRITICAL: Alpine launch failed with status $status. Dropping to recovery shell."
        export ALPINE_FAILSAFE=1
    fi
fi
