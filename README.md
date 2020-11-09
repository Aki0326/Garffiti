gARffiti
===============

ARCoreを用いて平面・垂直面に落書きできるアプリ

## Description
スマートフォンの画面に映っている壁, 床, 机, どこにでも自由に落書きできるARアプリです. 
落書きは, 検出した水平面・垂直面に固定されているため, 動き回って落書きする体験ができます. 

gARffitiを起動するとモード選択画面が表示されます. モードには, 「らくがき」, 「ぬりえバトル」,
「写真ギャラリー」の3 つがあります. 

- らくがきモード  
らくがきモードでは, 周囲の環境内で検出した水平面・垂直面にタッチして落書きができます. 

- ぬりえバトルモード  
ぬりえバトルでは, 周囲の環境内で検出した水平面・垂直面に落書きしたCG モデルを2 台
の端末で共有し, 一定時間内に落書きした面積を競う予定です. 

- 写真ギャラリーモード  
写真ギャラリーモードでは, らくがきモードで保存した画面のスクリーンショット写真の一覧を閲覧できます. 

<div align="center">
<img src="https://github.com/Aki0326/Garffiti/blob/developer_console/app/src/main/res/drawable-xxhdpi/mode_select.png" alt="モード選択画面" title="モード選択画面" width="200"><img src="https://github.com/Aki0326/Garffiti/blob/developer_console/app/src/main/res/drawable-xxhdpi/graffiti_mode.png" alt="らくがきモードのスクリーンショット" title="らくがきモードのスクリーンショット" width="200">
</div>

現在制作途中です．
GitBucketから移行してきました(Aki0326とAki Hongoは同一人物です). 
GitHub, Google Playストアでも公開しています．
  
  - [GitHub @Aki0326](https://github.com/Aki0326/gARffiti)
  
  - [Google PlayストアgARffiti<修正中>](https://play.google.com/store/apps/details?id=org.ntlab.graffiti&hl=ja)

## Requirement
- Android Studio 3.5.3

- Android SDK Platform 7.0以降

- Java 8以降

- Android端末のAPI 24以降(Pixel 3では動作確認済み)

## Usage
プロジェクトをビルドするには, ダウンロードしてAndroid Studio3.1以降で開きます. 
アプリを起動すると, ARCoreがまだインストールされていない場合はGoogle Playストアでインストールするように求められます. 
また, 端末付属カメラへの使用許可を求められます([プライバシーポリシー](https://photos.app.goo.gl/3HR5E2rffET9aUCj8)). 

## References
- ARCore
  https://developers.google.com/ar

- Aki0326/Garffiti/app/src/main/res/drawable-xxhdpi/  
  photoAC (https://www.photo-ac.com/)  
  justaline-android (https://github.com/googlecreativelab/justaline-android)
  
- Garffiti/app/src/main/assets/musics/
