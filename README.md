# react-native-photoedit-sdk

## Getting started

##### 💥 working only for android

`$ npm install react-native-photoedit-sdk --save`

`$ yarn add react-native-photoedit-sdk`

## Usage
```javascript
import PhotoeditSdk from "react-native-photoedit-sdk";
    PhotoEditSDK.Edit({
    path: filename,
    visible_gallery: false,
    visible_camera: false,
    draw_watermark: true,
    watermark: require('../assets/image/logo.png'),
    confirm_message: "Are you sure to send?",
    onDone: (uri) => {
        console.log("success", uri);
    },
    onCancel: (code) => console.log("err", code),
    });
```
## Params:
| param | type  | description  |
| ------------ | ------------ | ------------ |
|  path   | string  | edit image path  |
|  visible_gallery | boolean  | visible open gallery button  |
|  visible_camera | boolean  | visible open camera button  |
|  draw_watermark |  boolean | draw watermark  |
|  watermark |  integer  | watermark path(react native assets)  |
|  confirm_message |  string | photo editor confirm message  |
|  onDone  |  callback |  complete photo editor |
|  onCancel |  callback  | cancel photo editor  |


add lines on **AndroidManifest.xml**:
```xml
    <activity android:name="com.sunny.photoeditor.EditImageActivity" />
    <activity android:name="com.sunny.photoeditor.base.BaseActivity" />
```
