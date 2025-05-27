//
//  ImagePicker.swift
//  FoodGuide
//
//  Created by Aaron212 on 2025-04-30.
//

import SwiftUI
import UIKit

struct ImagePicker: UIViewControllerRepresentable {

    @Binding var selectedImage: UIImage?  // To send the selected image back to SwiftUI
    @Environment(\.presentationMode) var presentationMode  // To dismiss the picker
    var sourceType: UIImagePickerController.SourceType = .camera  // Default to camera

    // --- UIViewControllerRepresentable Required Methods ---

    // Creates the initial UIKit ViewController
    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = sourceType
        picker.delegate = context.coordinator  // Set the delegate to handle events
        // picker.allowsEditing = true // Optional: Allow basic editing
        return picker
    }

    // Updates the ViewController when SwiftUI state changes (often not needed for simple pickers)
    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {
        // No update needed in this basic example
    }

    // --- Coordinator ---
    // Acts as the delegate for the UIImagePickerController
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    // The Coordinator class handles delegate callbacks
    final class Coordinator: NSObject, UIImagePickerControllerDelegate,
        UINavigationControllerDelegate
    {
        var parent: ImagePicker

        init(_ parent: ImagePicker) {
            self.parent = parent
        }

        // Called when an image is picked
        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            // Try to get the original image
            if let image = info[.originalImage] as? UIImage {
                parent.selectedImage = image  // Update the binding
            }
            // Fallback or handle edited image if allowsEditing was true
            // else if let editedImage = info[.editedImage] as? UIImage {
            //     parent.selectedImage = editedImage
            // }

            parent.presentationMode.wrappedValue.dismiss()  // Dismiss the picker
        }

        // Called when the user cancels
        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.presentationMode.wrappedValue.dismiss()  // Dismiss the picker
        }
    }
}
