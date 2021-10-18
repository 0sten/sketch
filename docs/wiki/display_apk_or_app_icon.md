# 显示 APK 或已安装 APP 的图标

### 显示 APK 的图标

[Sketch] 支持显示 APK 文件的图标，是通过 [ApkIconUriModel] 实现的此功能，uri 如下:

```
apk.icon:///sdcard/file.apk
```

[SketchImageView]、[Sketch] 提供了相应的方法可供使用，如下：

```java
Sting apkIconUri = ApkIconUriModel.makeUri("/sdcard/file.apk");

// SketchImageView
SketchImageView sketchImageView = ...;
sketchImageView.displayImage(apkIconUri);

// Sketch.display()
Sketch.with(context).display(apkIconUri, sketchImageView).commit();

// Sketch.load()
Sketch.with(context).load(apkIconUri, new LoadListener(){...}).commit();
```

### 显示 APP 的图标

[Sketch] 还支持显示已安装 APP 的图标，是通过 [AppIconUriModel] 实现的此功能，uri 如下:

```
app.icon://com.github.panpf.sketch.sample/2500
```

[SketchImageView]、[Sketch] 提供了相应的方法可供使用，如下：

```java
Sting appIconUri = AppIconUriModel.makeUri("com.github.panpf.sketch.sample", 2500);

// SketchImageView
SketchImageView sketchImageView = ...;
sketchImageView.displayImage(appIconUri);

// Sketch.display()
Sketch.with(context).display(appIconUri, sketchImageView).commit();

// Sketch.load()
Sketch.with(context).load(appIconUri, new LoadListener(){...}).commit();
```

[Sketch]: ../../sketch/src/main/java/com/github/panpf/sketch/Sketch.java
[ApkIconUriModel]: ../../sketch/src/main/java/com/github/panpf/sketch/uri/ApkIconUriModel.java
[AppIconUriModel]: ../../sketch/src/main/java/com/github/panpf/sketch/uri/AppIconUriModel.java
[SketchImageView]: ../../sketch/src/main/java/com/github/panpf/sketch/SketchImageView.java
[Sketch]: ../../sketch/src/main/java/com/github/panpf/sketch/Sketch.java
