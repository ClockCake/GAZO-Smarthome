import 'package:connectivity_plus/connectivity_plus.dart';
import 'dart:async';

class NetworkStateHelper {
  static StreamSubscription<List<ConnectivityResult>>? _subscription;

  static void initNetworkListener(Function refreshCallback) {
    // 始终监听网络变化
    _subscription = Connectivity().onConnectivityChanged.listen((List<ConnectivityResult> results) {
      if (!results.contains(ConnectivityResult.none)) {
        // 当网络连接恢复时，执行刷新回调
        refreshCallback();
      }
    });
  }

  static void dispose() {
    _subscription?.cancel();
  }
}