import { NativeModules, Image, Platform } from 'react-native';
const { PhotoeditSdk } = NativeModules;
const { resolveAssetSource } = Image

export interface PhotoEditorProps {
    path: string
    visible_gallery: boolean
    visible_camera: boolean
    draw_watermark: boolean
    watermark?: number
    onDone?: (imagePath: string) => void
    onCancel?: (resultCode: number) => void
}

export default class PhotoEditor {
    static Edit({
        onDone,
        onCancel,
        ...props
    }: PhotoEditorProps) {
        let watermark = resolveAssetSource(props.watermark);
        PhotoeditSdk.Edit(
            { ...props, watermark:watermark.uri },
            (imagePath: string) => {
                onDone && onDone(imagePath)
            },
            (resultCode: number) => {
                onCancel && onCancel(resultCode)
            }
        )
    }
}
