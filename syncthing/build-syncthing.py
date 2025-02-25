from __future__ import print_function

import os
import os.path
import platform
import subprocess
import sys

PLATFORM_DIRS = {
    'Windows': 'windows-x86_64',
    'Linux': 'linux-x86_64',
    'Darwin': 'darwin-x86_64',
}

# The values here must correspond with those in ../docker/prebuild.sh
BUILD_TARGETS = [
    {
        'arch': 'arm',
        'goarch': 'arm',
        'jni_dir': 'armeabi',
        'cc': 'armv7a-linux-androideabi{}-clang',
    },
    {
        'arch': 'arm64',
        'goarch': 'arm64',
        'jni_dir': 'arm64-v8a',
        'cc': 'aarch64-linux-android{}-clang',
    },
    {
        'arch': 'x86',
        'goarch': '386',
        'jni_dir': 'x86',
        'cc': 'i686-linux-android{}-clang',
    },
    {
        'arch': 'x86_64',
        'goarch': 'amd64',
        'jni_dir': 'x86_64',
        'cc': 'x86_64-linux-android{}-clang',
    }
]


def fail(message, *args, **kwargs):
    print((message % args).format(**kwargs))
    sys.exit(1)


def get_min_sdk(project_dir):
    with open(os.path.join(project_dir, 'app', 'build.gradle.kts')) as file_handle:
        for line in file_handle:
            tokens = list(filter(None, line.split()))
            if len(tokens) == 3 and tokens[0] == 'minSdk':
                return int(tokens[2])

    fail('Failed to find minSdkVersion')


def get_ndk_home():
    if not os.environ.get('ANDROID_NDK_HOME', ''):
        if not os.environ.get('NDK_VERSION', '') or not os.environ.get('ANDROID_HOME', ''):
            fail('ANDROID_NDK_HOME or NDK_VERSION and ANDROID_HOME environment variable must be defined')
        return os.path.join(os.environ['ANDROID_HOME'], 'ndk', os.environ['NDK_VERSION'])
    return os.environ['ANDROID_NDK_HOME']

if platform.system() not in PLATFORM_DIRS:
    fail('Unsupported python platform %s. Supported platforms: %s', platform.system(),
         ', '.join(PLATFORM_DIRS.keys()))

module_dir = os.path.dirname(os.path.realpath(__file__))
project_dir = os.path.realpath(os.path.join(module_dir, '..'))
# Use separate build dir so standalone ndk isn't deleted by `gradle clean`
build_dir = os.path.join(module_dir, 'gobuild')
go_build_dir = os.path.join(build_dir, 'go-packages')
syncthing_dir = os.path.join(module_dir, 'src', 'github.com', 'syncthing', 'syncthing')
min_sdk = get_min_sdk(project_dir)

# Make sure all tags are available for git describe
# https://github.com/syncthing-android/syncthing-android/issues/872
subprocess.check_call([
    'git',
    '-C',
    syncthing_dir,
    'fetch',
    '--tags'
])

for target in BUILD_TARGETS:

    print('Building syncthing for', target['arch'])

    environ = os.environ.copy()
    environ.update({
        'GO111MODULE': 'on',
        'CGO_ENABLED': '1',
    })

    subprocess.check_call(
        ['go', 'version'],
        env=environ, cwd=syncthing_dir)
    subprocess.check_call(
        ['go', 'run', 'build.go', 'version'],
        env=environ, cwd=syncthing_dir)
    cc = os.path.join(
        get_ndk_home(), "toolchains", "llvm", "prebuilt",
        PLATFORM_DIRS[platform.system()], "bin",
        target['cc'].format(min_sdk))
    subprocess.check_call(
        ['go', 'run', 'build.go', '-goos', 'android',
         '-goarch', target['goarch'], '-cc', cc,
         '-pkgdir', os.path.join(go_build_dir, target['goarch']),
         '-no-upgrade', 'build'],
        env=environ, cwd=syncthing_dir)

    # Copy compiled binary to jniLibs folder
    target_dir = os.path.join(project_dir, 'app', 'src', 'main', 'jniLibs', target['jni_dir'])
    if not os.path.isdir(target_dir):
        os.makedirs(target_dir)
    target_artifact = os.path.join(target_dir, 'libsyncthing.so')
    if os.path.exists(target_artifact):
        os.unlink(target_artifact)
    os.rename(os.path.join(syncthing_dir, 'syncthing'), target_artifact)

    print('Finished build for', target['arch'])

print('All builds finished')
