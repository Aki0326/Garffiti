gARffiti
===============

平面上に仮想的な落書きができるARアプリ

## Description
スマートフォンのカメラを通して映っている壁, 床, 机, どこにでも自由に落書きできるARアプリです. 
落書きは, 検出した水平面・垂直面に固定されているため, 動き回って落書きする体験ができます. 
本アプリは, Googleが提供しているARプラットフォームARCoreを用いており, 環境理解, 光源推定などの機能を利用して開発しています. 

gARffitiを起動するとモード選択画面が表示されます. モードは, 「らくがき」, 「2人でらくがき」, 「らくがきタイムアタック」, 「写真ギャラリー」の4つあります. 

- らくがきモード

  周囲の環境内で検出した水平面・垂直面にタッチして落書きができます. 

- 2人でらくがきモード<実装中>

  2台の端末で周囲の環境内で検出した水平面・垂直面を共有しながら落書きすることができます. 

- らくがきタイムアタックモード<実装中>

  一定時間内に落書きした面積を競います. 

- 写真ギャラリーモード

  らくがきモードで保存した画面のスクリーンショット写真の一覧を閲覧できます. 

<div align="center">
<img src="https://github.com/Aki0326/Garffiti/blob/tuning/app/src/main/res/drawable-xxhdpi/mode_select.png" alt="モード選択画面" title="モード選択画面" width="200"><img src="https://github.com/Aki0326/Garffiti/blob/developer_console/app/src/main/res/drawable-xxhdpi/graffiti_mode.png" alt="らくがきモードのスクリーンショット" title="らくがきモードのスクリーンショット" width="200">
</div>

## Note
現在制作途中です．
GitBucketから移行してきました. 

  - [GitHub @Aki0326](https://github.com/Aki0326/gARffiti)
  
  - [Google PlayストアgARffiti<修正中>](https://play.google.com/store/apps/details?id=org.ntlab.graffiti&hl=ja)

### Author
- @Aki0326

  ※Aki0326とコミット履歴のAki Hongoは同一人物です. 

- @n-nitta

GitHub, Google Playストアでも公開しています．

### Branch
- master

  最新版

- developer_console

  Google Playストアにて公開しているバージョン

### 開発期間
- らくがきモード

  2019/5/29～8/20, 2021/2/5～3/17

- 2人でらくがきモード

  2020/2/26～4/3, 2021/3/17～

- らくがきタイムアタックモード

  2019/5/29～8/20, 2021/4/5～

- 写真ギャラリーモード  
2019/5/29～8/20

## Requirement
- Android Studio 3.5.3以降(Android Studio 4.1.3では動作確認済み)

- Java 8以降

- Android SDK Platform 9.0以降

- Android端末のAPI 28以降(Pixel 3では動作確認済み)

## Usage
1. プロジェクトをビルドするには, ダウンロードしてAndroid Studioで開きます. 

  `$ git clone https://github.com/Aki0326/Garffiti.git`

2. 端末にアプリをインストールして起動すると, ARCoreがまだインストールされていない場合はGoogle Playストアでインストールするように求められます. [対応機種について](https://developers.google.com/ar/discover/supported-devices)

3. 端末付属カメラへの使用許可を求められます. [[プライバシーポリシー](https://photos.app.goo.gl/3HR5E2rffET9aUCj8)]

## References
- ARCore
  (https://developers.google.com/ar)

- Aki0326/Garffiti/app/src/main/res/drawable-xxhdpi/

  photoAC (https://www.photo-ac.com/)
  
  justaline-android (https://github.com/googlecreativelab/justaline-android)
  
- Garffiti/app/src/main/assets/musics/

- ARCore Sample
  (https://github.com/google-ar/arcore-android-sdk)
  
- FuriganaView
  (https://github.com/sh0/furigana-view)