import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_smarthome/controllers/home_search.dart';
import 'package:flutter_smarthome/controllers/shopping_car_list.dart';
import 'package:flutter_smarthome/controllers/shopping_category_list.dart';
import 'package:flutter_smarthome/network/api_manager.dart';
import 'package:flutter_smarthome/utils/custom_navbar.dart';
import 'package:flutter_smarthome/controllers/shopping_home_list.dart';
import 'package:flutter_smarthome/utils/network_state_helper.dart';

/// 方案二：NestedScrollView + SliverPersistentHeader 修复 TabBarView 与 ListView 手势冲突
class ShoppingHomeWidget extends StatefulWidget {
  const ShoppingHomeWidget({Key? key}) : super(key: key);

  @override
  State<ShoppingHomeWidget> createState() => _ShoppingHomeWidgetState();
}

class _ShoppingHomeWidgetState extends State<ShoppingHomeWidget>
    with SingleTickerProviderStateMixin {
  List<String> _categoryNameList = [];
  List<String> _categoryIds = [];
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _initNetworkListener();
    _getCategory();
  }

  void _initNetworkListener() {
    NetworkStateHelper.initNetworkListener(() {
      if (mounted) {
        _getCategory();
      }
    });
  }

  void _initTabController() {
    _tabController = TabController(
      length: _categoryNameList.length,
      vsync: this,
    );
    _tabController.addListener(() => setState(() {}));
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
        title: '极家Life',
        onSearchTap: () {
          Navigator.push(
            context,
            MaterialPageRoute(builder: (_) => const HomeSearchPage(type: 2)),
          );
        },
        onCartTap: () {
          Navigator.push(
            context,
            MaterialPageRoute(builder: (_) => const ShoppingCarListWidget()),
          );
        },
      ),
      body: SafeArea(
        child: _categoryNameList.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : NestedScrollView(
                headerSliverBuilder: (context, innerBoxIsScrolled) {
                  return [
                    SliverPersistentHeader(
                      pinned: true,
                      delegate: _TabBarSliverDelegate(_buildTabBar()),
                    ),
                  ];
                },
                body: TabBarView(
                  controller: _tabController,
                  children: _categoryIds
                      .map((id) => ShoppingHomeListWidget(categoryId: id))
                      .toList(),
                ),
              ),
      ),
    );
  }

  /// 将 TabBar 容器包在 PreferredSize 中返回，满足 SliverPersistentHeader 要求
  PreferredSize _buildTabBar() {
    final bool isScrollable = _categoryNameList.length > 4;

    return PreferredSize(
      preferredSize: Size.fromHeight(44.h),
      child: Container(
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
              child: TabBar(
                controller: _tabController,
                isScrollable: isScrollable,
                dividerHeight: 0,
                labelPadding: EdgeInsets.zero,
                tabs: _categoryNameList.asMap().entries.map((entry) {
                  final index = entry.key;
                  final title = entry.value;
                  final isSelected = index == _tabController.index;
                  final isFirst = index == 0;
                  
                  return Tab(
                    child: Container(
                      padding: EdgeInsets.symmetric(
                        horizontal: isFirst ? 0.w : 16.w,
                      ),
                      child: Center(
                        child: Text(
                          title,
                          style: TextStyle(
                            fontWeight:
                                isSelected ? FontWeight.bold : FontWeight.normal,
                          ),
                        ),
                      ),
                    ),
                  );
                }).toList(),
                labelColor: Colors.black,
                unselectedLabelColor: Colors.grey,
                indicatorColor: Colors.orange,
              ),
            ),
            Padding(
              padding: EdgeInsets.symmetric(horizontal: 12.w),
              child: InkWell(
                onTap: () => Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (_) => const ShoppingCategoryListWidget(),
                  ),
                ),
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
                data.map((e) => e['categoryName'] as String).toList();
            _categoryIds =
                data.map((e) => e['categoryId'].toString()).toList();
            _initTabController();
          });
        }
      }
    } catch (e) {
      debugPrint('获取分类数据错误: $e');
    }
  }
}

/// SliverPersistentHeaderDelegate 实现，使用 PreferredSizeWidget
class _TabBarSliverDelegate extends SliverPersistentHeaderDelegate {
  final PreferredSizeWidget tabBar;

  _TabBarSliverDelegate(this.tabBar);

  @override
  double get minExtent => tabBar.preferredSize.height;

  @override
  double get maxExtent => tabBar.preferredSize.height;

  @override
  Widget build(
      BuildContext context, double shrinkOffset, bool overlapsContent) {
    return Material(color: Colors.white, child: tabBar);
  }

  @override
  bool shouldRebuild(covariant _TabBarSliverDelegate oldDelegate) {
    return false;
  }
}
