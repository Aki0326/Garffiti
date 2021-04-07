gARffiti
===============

ARCoreを用いて水平面・垂直面に仮想的な落書きができるAndroidアプリ

## Description
スマートフォンの画面に映っている壁, 床, 机, どこにでも自由に落書きできるARアプリです. 
落書きは, 検出した水平面・垂直面に固定されているため, 動き回って落書きする体験ができます. 

gARffitiを起動するとモード選択画面が表示されます. モードには, 「らくがき」, 「2人でらくがき」, 「らくがきタイムアタック」, 「写真ギャラリー」の4つがあります. 

- らくがきモード
らくがきモードでは, 周囲の環境内で検出した水平面・垂直面にタッチして落書きができます. 

- 2人でらくがきモード<実装中>
2人でらくがきモードでは, 2台の端末で共有しながら周囲の環境内で検出した水平面・垂直面に落書きすることができます. 

- らくがきタイムアタックモード<実装中>
らくがきタイムアタックモードでは, 一定時間内に落書きできた面積を競う予定です. 

- 写真ギャラリーモード  
写真ギャラリーモードでは, らくがきモードで保存した画面のスクリーンショット写真の一覧を閲覧できます. 

<div align="center">
<img src="https://github.com/Aki0326/Garffiti/blob/tuning/app/src/main/res/drawable-xxhdpi/mode_select.png" alt="モード選択画面" title="モード選択画面" width="200"><img src="https://github.com/Aki0326/Garffiti/blob/developer_console/app/src/main/res/drawable-xxhdpi/graffiti_mode.png" alt="らくがきモードのスクリーンショット" title="らくがきモードのスクリーンショット" width="200">
</div>

## Note
現在制作途中です．
GitBucketから移行してきました(Aki0326とAki Hongoは同一人物です). 

GitHub, Google Playストアでも公開しています．

  - [GitHub @Aki0326](https://github.com/Aki0326/gARffiti)
  
  - [Google PlayストアgARffiti<修正中>](https://play.google.com/store/apps/details?id=org.ntlab.graffiti&hl=ja)

デモ動画も公開しています. 

## Requirement
- Android Studio 3.5.3以降(Android Studio 4.1.3では動作確認済み)

- Android SDK Platform 9.0以降

- Java 8以降

- Android端末のAPI 28以降(Pixel 3では動作確認済み)

## Usage
プロジェクトをビルドするには, ダウンロードしてAndroid Studioで開きます. 
端末にアプリをインストールして起動すると, ARCoreがまだインストールされていない場合はGoogle Playストアでインストールするように求められます. 
また, 端末付属カメラへの使用許可を求められます([プライバシーポリシー](https://photos.app.goo.gl/3HR5E2rffET9aUCj8)). 

## References
- ARCore
  https://developers.google.com/ar

- Aki0326/Garffiti/app/src/main/res/drawable-xxhdpi/  
  photoAC https://www.photo-ac.com/
  justaline-android https://github.com/googlecreativelab/justaline-android
  
- Garffiti/app/src/main/assets/musics/

- ARCore Sample
  https://github.com/google-ar/arcore-android-sdk