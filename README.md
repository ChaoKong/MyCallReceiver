# MyCallReceiver


The project is developed by Eclipse + adt.

In this project, it uses google play service. Here is a link of how to adopt google play service to a project. 
https://developers.google.com/android/guides/setup#add_google_play_services_to_your_project

In addition, the project uses native code. We can use Native Development Kit (NDK) to build it.
Here are the links of how to use NDK.
http://developer.android.com/ndk/guides/index.html

http://developer.android.com/ndk/guides/setup.html

For this project, I use the android-ndk-r10e. I put the whole "MyCallReceiver" project in the directory:
"../android-ndk-r10e/samples/MyCallReceiver", then in this directory, run the command "ndk-build" to compile
the native code. After that, we can run the project in Eclipse.


