# Add project specific ProGuard rules here.

# Keep ONNX Runtime classes
-keep class ai.onnxruntime.** { *; }

# Keep Room entities
-keep class org.bhashasetu.fln.edge.data.** { *; }

# Keep data classes used in serialization
-keep class org.bhashasetu.fln.edge.sync.P2PSyncManager$SyncRequest { *; }
-keep class org.bhashasetu.fln.edge.sync.P2PSyncManager$SyncResponse { *; }