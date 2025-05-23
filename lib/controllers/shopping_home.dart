import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_smarthome/controllers/home_search.dart';
import 'package:flutter_smarthome/controllers/shopping_car_list.dart';
import 'package:flutter_smarthome/controllers/shopping_category_list.dart';
import 'package:flutter_smarthome/network/api_manager.dart';
import 'package:flutter_smarthome/utils/custom_navbar.dart';
import 'package:flutter_smarthome/controllers/shopping_home_list.dart';
import 'package:flutter_smarthome/controllers/shopping_category_list.dart';
import 'package:flutter_smarthome/utils/network_state_helper.dart';

class ShoppingHomeWidget extends StatefulWidget {
  const ShoppingHomeWidget({Key? key}) : super(key: key);

  @override
  State<ShoppingHomeWidget> createState() => _ShoppingHomeWidgetState();
}

class _ShoppingHomeWidgetState extends State<ShoppingHomeWidget>
    with SingleTickerProviderStateMixin {

  List<String> _categoryNameList = []; // 品类名
  List<String> _categoryIds = []; // 品类id
  late TabController _tabController;
  bool _isInitialLoad = true;

  @override
  void initState() {
    super.initState();
      _initNetworkListener();  // ← 调用网络状态监听
    _loadInitialData();

  }

  void _initNetworkListener() {
   NetworkStateHelper.initNetworkListener(() {
      if (mounted) {
        // 网络从无到有时自动下拉刷新
        _getCategory();
      }
    });
  }

  Future<void> _loadInitialData() async {
    try {
      await Future.wait([
        _getCategory(),
        // 其他需要初始化的数据加载
      ]);
    } catch (e) {
      // 处理错误情况
    } finally {
      // 初始加载完成后，设置标志位为false
      setState(() {
        _isInitialLoad = false;
      });
    }
  }

  void _initTabController() {
    _tabController = TabController(
      length: _categoryNameList.length,
      vsync: this,
    );

    // 添加监听器
    _tabController.addListener(() {
      setState(() {}); // 当 Tab 切换时，重建以更新字体样式
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: CustomNavigationBar(
        title: "极家Life",
        onSearchTap: () {
          // 处理搜索按钮点击
          Navigator.push(context, MaterialPageRoute(builder: (context) => HomeSearchPage(type: 2)));
        },
        onCartTap: () {
          Navigator.push(context, MaterialPageRoute(builder: (context) => ShoppingCarListWidget()));
        },
      ),
      body: SafeArea(
        child: _categoryNameList.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : Column(
                children: [
                  Container(
                    height: 44.h,
                    decoration: BoxDecoration(
                      color: Colors.white,
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withOpacity(0.1),
                          blurRadius: 4,
                          offset: const Offset(0, 2),
                        ),
                      ],
                    ),
                    child: Row(
                      children: [
                        Expanded(
                          child: LayoutBuilder(
                            builder: (context, constraints) {
                              int tabCount = _categoryNameList.length;
                              bool isScrollable = tabCount > 4;
                              double totalWidth = constraints.maxWidth;
                              double tabWidth = totalWidth /
                                  (isScrollable ? 4 : tabCount);
                              return TabBar(
                                controller: _tabController,
                                isScrollable: isScrollable,
                                dividerHeight: 0,
                                labelPadding: EdgeInsets.zero,
                                tabs: List.generate(_categoryNameList.length,
                                    (index) {
                                  String title = _categoryNameList[index];
                                  bool isSelected =
                                      _tabController.index == index;
                                  return SizedBox(
                                    width: tabWidth,
                                    child: Tab(
                                      child: Center(
                                        child: Text(
                                          title,
                                          style: TextStyle(
                                            fontWeight: isSelected
                                                ? FontWeight.bold
                                                : FontWeight.normal,
                                          ),
                                        ),
                                      ),
                                    ),
                                  );
                                }),
                                labelColor: Colors.black,
                                unselectedLabelColor: Colors.grey,
                                indicator: UnderlineTabIndicator(
                                  borderSide: const BorderSide(
                                    width: 3,
                                    color: Colors.orange,
                                  ),
                                  insets: EdgeInsets.symmetric(
                                    horizontal: (tabWidth - 24) / 2,
                                  ),
                                ),
                              );
                            },
                          ),
                        ),
                        Container(
                          padding: EdgeInsets.symmetric(horizontal: 12.w),
                          child: InkWell(
                            onTap: () {                    
                              Navigator.push(context, MaterialPageRoute(builder: (context) => ShoppingCategoryListWidget()));
                            },
                            child: Image.asset(
                              'assets/images/icon_shopping_more.png',
                              width: 24.w,
                              height: 24.h,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  Expanded(
                    child: TabBarView(
                      controller: _tabController,
                      children: _categoryIds
                          .map((id) => ShoppingHomeListWidget(categoryId: id))
                          .toList(),
                    ),
                  ),
                ],
              ),
      ),
    );
  }

  Future<void> _getCategory() async {
    try {
      final apiManager = ApiManager();
      final response = await apiManager.get('/api/shopping/category');

      if (response != null && mounted) {
        final List<dynamic> data = response as List<dynamic>;

        if (data.isNotEmpty) {
          setState(() {
            _categoryNameList =
                data.map((item) => item['categoryName'] as String).toList();
            _categoryIds =
                data.map((item) => item['categoryId'].toString()).toList();
          });
          _initTabController(); // 初始化 TabController
        }
      }
    } catch (e) {
      print('获取分类数据错误: $e');
      // 可以添加错误提示
      // if (mounted) {
      //   ScaffoldMessenger.of(context).showSnackBar(
      //     const SnackBar(content: Text('获取分类数据失败，请稍后重试')),
      //   );
      // }
    }
  }
 
}