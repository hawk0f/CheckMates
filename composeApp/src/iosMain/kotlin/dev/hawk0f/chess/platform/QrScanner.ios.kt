package dev.hawk0f.chess.platform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureOutput
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun QrScannerView(
    onResult: (String) -> Unit,
    onPermissionDenied: () -> Unit
) {
    var authorized by remember {
        mutableStateOf(
            AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized
        )
    }

    LaunchedEffect(Unit) {
        if (!authorized) {
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                dispatch_get_main_queue().let {
                    if (granted) {
                        authorized = true
                    } else {
                        onPermissionDenied()
                    }
                }
            }
        }
    }

    if (!authorized) {
        return
    }

    val session = remember { AVCaptureSession() }
    var delivered by remember { mutableStateOf(false) }
    val delegate = remember {
        object : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
            override fun captureOutput(
                output: AVCaptureOutput,
                didOutputMetadataObjects: List<*>,
                fromConnection: AVCaptureConnection
            ) {
                val value = didOutputMetadataObjects
                    .filterIsInstance<AVMetadataMachineReadableCodeObject>()
                    .firstOrNull()
                    ?.stringValue
                if (value != null && !delivered) {
                    delivered = true
                    onResult(value)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            session.stopRunning()
        }
    }

    UIKitView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            val view = UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))
            val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
            if (device != null) {
                val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null)
                if (input != null && session.canAddInput(input)) {
                    session.addInput(input)
                }
                val output = AVCaptureMetadataOutput()
                if (session.canAddOutput(output)) {
                    session.addOutput(output)
                    output.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
                    output.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
                }
                val previewLayer = AVCaptureVideoPreviewLayer(session = session)
                previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
                previewLayer.frame = view.bounds
                view.layer.addSublayer(previewLayer)
                session.startRunning()
            }
            view
        },
        update = { view ->
            view.layer.sublayers?.firstOrNull()?.let { layer ->
                (layer as? AVCaptureVideoPreviewLayer)?.setFrame(view.bounds)
            }
        }
    )
}
