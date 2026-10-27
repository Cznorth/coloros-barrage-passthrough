#!/bin/bash
# 构建 barrage-passthrough.apk（不需要 Gradle）
# 需要：JDK 8+、Android SDK（build-tools 34+、任意 platforms/android-3x）
#   ANDROID_HOME   SDK 路径（默认 ~/Android/Sdk）
#   KEYSTORE       签名用 keystore（默认 ~/.android/debug.keystore，密码 android）
# WSL 下也可以直接用 Windows 端的 SDK/JDK（自动识别 *.exe）
set -e
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:-$HOME/Android/Sdk}
BT=$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)
JAR=$(ls -d "$SDK"/platforms/android-*/android.jar | sort -V | tail -1)
KS=${KEYSTORE:-$HOME/.android/debug.keystore}
OUT=$PWD/build; rm -rf "$OUT"; mkdir -p "$OUT"/{stubs,cls,dex}

if [ -f "$BT/aapt2.exe" ]; then            # WSL + Windows SDK
  p() { wslpath -w "$1"; }; X=.exe; SEP=';'
  bat() { local t=$1; shift; cmd.exe /c "$(p "$BT/$t.bat")" "$@" 2>&1 | tr -d '\r'; }
else
  p() { echo "$1"; }; X=; SEP=':'
  bat() { local t=$1; shift; "$BT/$t" "$@"; }
fi

javac$X -encoding UTF-8 -source 8 -target 8 -nowarn -d "$(p "$OUT/stubs")" $(find stubs -name '*.java')
javac$X -encoding UTF-8 -source 8 -target 8 -nowarn -cp "$(p "$JAR")$SEP$(p "$OUT/stubs")" \
  -d "$(p "$OUT/cls")" $(find src -name '*.java')
(cd "$OUT/cls" && python3 -c "import zipfile,pathlib;z=zipfile.ZipFile('../cls.jar','w');[z.write(f,str(f)) for f in pathlib.Path('.').rglob('*.class')]")
(cd "$OUT" && bat d8 --release --min-api 26 --lib "$(p "$JAR")" --classpath stubs --output dex cls.jar)
"$BT/aapt2$X" compile --dir "$(p res)" -o "$(p "$OUT/res.zip")"
"$BT/aapt2$X" link -I "$(p "$JAR")" --manifest "$(p AndroidManifest.xml)" -A "$(p assets)" \
  --min-sdk-version 26 --target-sdk-version 35 -o "$(p "$OUT/u.apk")" "$(p "$OUT/res.zip")"
cd "$OUT"
python3 -c "import zipfile;zipfile.ZipFile('u.apk','a').write('dex/classes.dex','classes.dex')"
"$BT/zipalign$X" -f 4 u.apk a.apk
bat apksigner sign --ks "$(p "$KS")" --ks-pass pass:android --out barrage-passthrough.apk a.apk
echo "$OUT/barrage-passthrough.apk"
