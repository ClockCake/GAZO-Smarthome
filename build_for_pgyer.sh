#!/bin/bash
# 构建用于蒲公英分发的IPA
echo "开始构建用于蒲公英分发的IPA..."
flutter build ipa --export-method ad-hoc

echo "构建完成，IPA文件位于: $(pwd)/build/ios/ipa/"
echo "现在您可以将此IPA文件上传至蒲公英平台进行分发"
