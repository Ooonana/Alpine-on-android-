#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <jni.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>

#define ALPINE_UNUSED(x) x __attribute__((__unused__))
#ifdef __APPLE__
# define LACKS_PTSNAME_R
#endif

static int throw_runtime_exception(JNIEnv* env, char const* message)
{
    if ((*env)->ExceptionCheck(env)) return -1;
    jclass exClass = (*env)->FindClass(env, "java/lang/RuntimeException");
    if (exClass == NULL) return -1;
    (*env)->ThrowNew(env, exClass, message);
    (*env)->DeleteLocalRef(env, exClass);
    return -1;
}

static void free_string_array(char** values)
{
    if (values == NULL) return;
    for (char** value = values; *value != NULL; ++value) free(*value);
    free(values);
}

static char** copy_java_string_array(JNIEnv* env, jobjectArray source, char const* label)
{
    if (source == NULL) return NULL;

    jsize size = (*env)->GetArrayLength(env, source);
    if ((*env)->ExceptionCheck(env)) return NULL;

    char** result = (char**) calloc((size_t) size + 1, sizeof(char*));
    if (result == NULL) {
        throw_runtime_exception(env, "Couldn't allocate native string array");
        return NULL;
    }

    for (jsize i = 0; i < size; ++i) {
        jstring java_string = (jstring) (*env)->GetObjectArrayElement(env, source, i);
        if ((*env)->ExceptionCheck(env)) {
            free_string_array(result);
            return NULL;
        }
        if (java_string == NULL) {
            free_string_array(result);
            throw_runtime_exception(env, label);
            return NULL;
        }

        char const* utf8 = (*env)->GetStringUTFChars(env, java_string, NULL);
        if (utf8 == NULL) {
            (*env)->DeleteLocalRef(env, java_string);
            free_string_array(result);
            if (!(*env)->ExceptionCheck(env)) throw_runtime_exception(env, "GetStringUTFChars() failed");
            return NULL;
        }

        result[i] = strdup(utf8);
        (*env)->ReleaseStringUTFChars(env, java_string, utf8);
        (*env)->DeleteLocalRef(env, java_string);
        if (result[i] == NULL) {
            free_string_array(result);
            throw_runtime_exception(env, "Couldn't duplicate native string");
            return NULL;
        }
    }

    return result;
}

static int create_subprocess(JNIEnv* env,
        char const* cmd,
        char const* cwd,
        char* const argv[],
        char** envp,
        int* pProcessId,
        jint rows,
        jint columns,
        jint cell_width,
        jint cell_height)
{
    int ptm = open("/dev/ptmx", O_RDWR | O_CLOEXEC);
    if (ptm < 0) return throw_runtime_exception(env, "Cannot open /dev/ptmx");

#ifdef LACKS_PTSNAME_R
    char* devname;
#else
    char devname[64];
#endif
    if (grantpt(ptm) || unlockpt(ptm) ||
#ifdef LACKS_PTSNAME_R
            (devname = ptsname(ptm)) == NULL
#else
            ptsname_r(ptm, devname, sizeof(devname))
#endif
       ) {
        close(ptm);
        return throw_runtime_exception(env, "Cannot grantpt()/unlockpt()/ptsname_r() on /dev/ptmx");
    }

    // Enable UTF-8 mode and disable flow control to prevent Ctrl+S from locking up the display.
    struct termios tios = {0};
    if (tcgetattr(ptm, &tios) != 0) {
        close(ptm);
        return throw_runtime_exception(env, "tcgetattr() failed for /dev/ptmx");
    }
    tios.c_iflag |= IUTF8;
    tios.c_iflag &= ~(IXON | IXOFF);
    if (tcsetattr(ptm, TCSANOW, &tios) != 0) {
        close(ptm);
        return throw_runtime_exception(env, "tcsetattr() failed for /dev/ptmx");
    }

    /** Set initial winsize. */
    struct winsize sz = { .ws_row = (unsigned short) rows, .ws_col = (unsigned short) columns, .ws_xpixel = (unsigned short) (columns * cell_width), .ws_ypixel = (unsigned short) (rows * cell_height)};
    ioctl(ptm, TIOCSWINSZ, &sz);

    pid_t pid = fork();
    if (pid < 0) {
        close(ptm);
        return throw_runtime_exception(env, "Fork failed");
    } else if (pid > 0) {
        *pProcessId = (int) pid;
        return ptm;
    } else {
        // Clear signals which the Android java process may have blocked:
        sigset_t signals_to_unblock;
        sigfillset(&signals_to_unblock);
        sigprocmask(SIG_UNBLOCK, &signals_to_unblock, 0);

        close(ptm);
        if (setsid() < 0) {
            perror("setsid()");
            _exit(1);
        }

        int pts = open(devname, O_RDWR);
        if (pts < 0) exit(-1);

        if (dup2(pts, STDIN_FILENO) < 0 ||
            dup2(pts, STDOUT_FILENO) < 0 ||
            dup2(pts, STDERR_FILENO) < 0) {
            perror("dup2()");
            _exit(1);
        }

        DIR* self_dir = opendir("/proc/self/fd");
        if (self_dir != NULL) {
            int self_dir_fd = dirfd(self_dir);
            struct dirent* entry;
            while ((entry = readdir(self_dir)) != NULL) {
                int fd = atoi(entry->d_name);
                if (fd > 2 && fd != self_dir_fd) close(fd);
            }
            closedir(self_dir);
        }

        clearenv();
        if (envp) for (; *envp; ++envp) putenv(*envp);

        if (chdir(cwd) != 0) {
            char* error_message;
            // No need to free asprintf()-allocated memory since doing execvp() or exit() below.
            if (asprintf(&error_message, "chdir(\"%s\")", cwd) == -1) error_message = "chdir()";
            perror(error_message);
            fflush(stderr);
        }
        execvp(cmd, argv);
        // Show terminal output about failing exec() call:
        char* error_message;
        if (asprintf(&error_message, "exec(\"%s\")", cmd) == -1) error_message = "exec()";
        perror(error_message);
        _exit(1);
    }
}

JNIEXPORT jint JNICALL Java_com_alpine_terminal_JNI_createSubprocess(
        JNIEnv* env,
        jclass ALPINE_UNUSED(clazz),
        jstring cmd,
        jstring cwd,
        jobjectArray args,
        jobjectArray envVars,
        jintArray processIdArray,
        jint rows,
        jint columns,
        jint cell_width,
        jint cell_height)
{
    if (cmd == NULL || cwd == NULL || args == NULL || processIdArray == NULL) {
        return throw_runtime_exception(env, "createSubprocess() received null required input");
    }
    jsize args_size = (*env)->GetArrayLength(env, args);
    if ((*env)->ExceptionCheck(env)) return -1;
    if (args_size < 1) {
        return throw_runtime_exception(env, "createSubprocess() requires at least argv[0]");
    }
    jsize process_id_size = (*env)->GetArrayLength(env, processIdArray);
    if ((*env)->ExceptionCheck(env)) return -1;
    if (process_id_size < 1) {
        return throw_runtime_exception(env, "createSubprocess() requires a processId output slot");
    }

    char const* cmd_utf8 = (*env)->GetStringUTFChars(env, cmd, NULL);
    if (cmd_utf8 == NULL) return -1;
    char const* cmd_cwd = (*env)->GetStringUTFChars(env, cwd, NULL);
    if (cmd_cwd == NULL) {
        (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf8);
        return -1;
    }

    char** argv = copy_java_string_array(env, args, "createSubprocess() argv contains null");
    if (argv == NULL) {
        (*env)->ReleaseStringUTFChars(env, cwd, cmd_cwd);
        (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf8);
        return -1;
    }
    char** envp = NULL;
    if (envVars != NULL) {
        envp = copy_java_string_array(env, envVars, "createSubprocess() env contains null");
        if (envp == NULL) {
            free_string_array(argv);
            (*env)->ReleaseStringUTFChars(env, cwd, cmd_cwd);
            (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf8);
            return -1;
        }
    }

    int procId = 0;
    int ptm = create_subprocess(env, cmd_utf8, cmd_cwd, argv, envp, &procId, rows, columns, cell_width, cell_height);
    (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf8);
    (*env)->ReleaseStringUTFChars(env, cwd, cmd_cwd);
    free_string_array(argv);
    free_string_array(envp);

    if (ptm < 0 || (*env)->ExceptionCheck(env)) return ptm;

    jint javaProcId = (jint) procId;
    (*env)->SetIntArrayRegion(env, processIdArray, 0, 1, &javaProcId);
    if ((*env)->ExceptionCheck(env)) {
        close(ptm);
        if (procId > 0) {
            kill(procId, SIGKILL);
            while (waitpid(procId, NULL, 0) < 0 && errno == EINTR) {}
        }
        return -1;
    }

    return ptm;
}

JNIEXPORT void JNICALL Java_com_alpine_terminal_JNI_setPtyWindowSize(JNIEnv* ALPINE_UNUSED(env), jclass ALPINE_UNUSED(clazz), jint fd, jint rows, jint cols, jint cell_width, jint cell_height)
{
    struct winsize sz = { .ws_row = (unsigned short) rows, .ws_col = (unsigned short) cols, .ws_xpixel = (unsigned short) (cols * cell_width), .ws_ypixel = (unsigned short) (rows * cell_height) };
    ioctl(fd, TIOCSWINSZ, &sz);
}

JNIEXPORT void JNICALL Java_com_alpine_terminal_JNI_setPtyUTF8Mode(JNIEnv* ALPINE_UNUSED(env), jclass ALPINE_UNUSED(clazz), jint fd)
{
    struct termios tios = {0};
    if (tcgetattr(fd, &tios) != 0) return;
    if ((tios.c_iflag & IUTF8) == 0) {
        tios.c_iflag |= IUTF8;
        tcsetattr(fd, TCSANOW, &tios);
    }
}

JNIEXPORT jint JNICALL Java_com_alpine_terminal_JNI_waitFor(JNIEnv* env, jclass ALPINE_UNUSED(clazz), jint pid)
{
    int status;
    pid_t result;
    do {
        result = waitpid(pid, &status, 0);
    } while (result < 0 && errno == EINTR);
    if (result < 0) return throw_runtime_exception(env, "waitpid() failed");
    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    } else if (WIFSIGNALED(status)) {
        return -WTERMSIG(status);
    } else {
        // Should never happen - waitpid(2) says "One of the first three macros will evaluate to a non-zero (true) value".
        return 0;
    }
}

JNIEXPORT void JNICALL Java_com_alpine_terminal_JNI_close(JNIEnv* ALPINE_UNUSED(env), jclass ALPINE_UNUSED(clazz), jint fileDescriptor)
{
    close(fileDescriptor);
}
