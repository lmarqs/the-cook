"""The bwrap sandbox each run and each judge call executes in.

The sandbox sees the system read-only, the run's repository at /work, the tool binaries, and a HOME that holds only
the two login files. The login files are bound, not copied, so a token refresh inside a run writes the real file.
"""

import os
import shutil
from pathlib import Path

HOME = Path.home()
CLAUDE_AUTH = HOME / ".claude" / ".credentials.json"
CODEX_AUTH = HOME / ".codex" / "auth.json"
EVAL_DIR = "/eval"


def resolved(binary: str) -> Path:
    path = shutil.which(binary)
    if not path:
        raise RuntimeError(f"{binary} is not on PATH")
    return Path(path).resolve()


def binaries() -> dict[str, Path]:
    return {"claude": resolved("claude"), "codex": resolved("codex"), "python3": resolved("python3")}


def tool_mounts() -> list[Path]:
    """The directories holding the binaries a run calls, each bound read-only at its own path."""
    found = binaries()
    mounts = {found["claude"].parent, found["codex"].parent.parent, found["python3"].parent.parent}
    linuxbrew = Path("/home/linuxbrew")
    if str(found["codex"]).startswith(str(linuxbrew)):
        mounts = {m for m in mounts if not str(m).startswith(str(linuxbrew))} | {linuxbrew}
    return sorted(mounts)


def command(work: Path, argv: list[str], *, eval_dir: Path | None = None, network: bool = True) -> list[str]:
    """The bwrap command line running `argv` in `work`."""
    found = binaries()
    dirs = [found["claude"].parent, found["codex"].parent, found["python3"].parent]
    dirs += [d for d in [Path("/home/linuxbrew/.linuxbrew/bin")] if d.is_dir()]
    path = ":".join(str(p) for p in dirs) + ":/usr/bin"
    args = ["bwrap", "--unshare-all", "--die-with-parent", "--new-session"]
    if network:
        args.append("--share-net")
    args += ["--ro-bind", "/usr", "/usr", "--ro-bind", "/etc", "/etc"]
    resolv = Path("/etc/resolv.conf").resolve()
    if resolv != Path("/etc/resolv.conf"):
        args += ["--ro-bind", str(resolv), str(resolv)]
    for link, target in [("/bin", "usr/bin"), ("/lib", "usr/lib"), ("/lib64", "usr/lib64"), ("/sbin", "usr/sbin")]:
        args += ["--symlink", target, link]
    args += ["--dev", "/dev", "--proc", "/proc", "--tmpfs", "/tmp", "--tmpfs", str(HOME)]
    for mount in tool_mounts():
        args += ["--ro-bind", str(mount), str(mount)]
    for auth in [CLAUDE_AUTH, CODEX_AUTH]:
        args += ["--dir", str(auth.parent), "--bind", str(auth), str(auth)]
    if eval_dir:
        args += ["--ro-bind", str(eval_dir), EVAL_DIR]
    args += ["--bind", str(work), "/work", "--chdir", "/work", "--clearenv"]
    env = {"HOME": str(HOME), "PATH": path, "TERM": "dumb", "LANG": "C.UTF-8", "USER": os.environ.get("USER", "user")}
    for key, value in env.items():
        args += ["--setenv", key, value]
    return args + argv
