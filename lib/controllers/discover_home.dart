
import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_smarthome/controllers/discover_infomation.dart';
import 'package:flutter_smarthome/controllers/furnish_form.dart';
import 'package:flutter_smarthome/controllers/home_search.dart';
import 'package:flutter_smarthome/utils/set_proxy_page.dart'; 
import 'discover_recommend.dart';

class DiscoverHomeWidget extends StatefulWidget {
  const DiscoverHomeWidget({super.key});

  @override
  State<DiscoverHomeWidget> createState() => _DiscoverHomeWidgetState();
}

class _DiscoverHomeWidgetState extends State<DiscoverHomeWidget> 
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  final List<String> _tabTitles = const ['推荐', '装修', '资讯'];
  
  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: _tabTitles.length, vsync: this);
    _tabController.addListener(() => setState(() {}));
  }
  
  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }
  
  void _navigateToSetProxy() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => SetProxyPage()),
    );
  }

  /// 将 TabBar 容器包在 PreferredSize 中返回，满足 SliverPersistentHeader 要求
  PreferredSize _buildTabBar() {
    return PreferredSize(
      preferredSize: Size.fromHeight(44.h),
      child: Container(
        height: 44.h,
        margin: EdgeInsets.symmetric(horizontal: 20.w),
        decoration: BoxDecoration(
          color: const Color(0xFFF8F2F8), // 淡紫色背景，与图片中一致
          borderRadius: BorderRadius.circular(22),
        ),
        child: TabBar(
          controller: _tabController,
          isScrollable: false, // 设为false以便标签等分
          dividerHeight: 0,
          padding: EdgeInsets.zero,
          // 移除标签间距，确保标签等分
          labelPadding: EdgeInsets.zero,
          indicatorPadding: EdgeInsets.zero,
          tabs: _tabTitles.map((title) {
            final isSelected = _tabTitles.indexOf(title) == _tabController.index;
            return Tab(
              height: 44.h,
              child: Text(
                title,
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                  color: isSelected ? Colors.black : Colors.grey,
                ),
              ),
            );
          }).toList(),
          // 移除原生指示器颜色，我们使用自定义指示器
          indicatorColor: Colors.transparent,
          labelColor: Colors.black,
          unselectedLabelColor: Colors.grey,
          // 自定义指示器，只显示文字宽度
          indicator: _CustomTabIndicator(
            color: Colors.black,
            height: 2.0,
            tabController: _tabController,
            indicatorWeight: 2.0,
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: GestureDetector(
        onLongPress: _navigateToSetProxy,
        child: SafeArea(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            children: [
              // 搜索框部分
              GestureDetector(
                onTap: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => HomeSearchPage(type: 1),
                    ),
                  );
                },
                child: Container(
                  padding: EdgeInsets.fromLTRB(20.w, 10.h, 20.w, 10.h),
                  child: Row(
                    children: [
                      Expanded(
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10),
                          height: 30.h,
                          decoration: BoxDecoration(
                            color: Colors.grey[200],
                            borderRadius: BorderRadius.circular(20),
                          ),
                          child: Row(
                            children: const [
                              Icon(Icons.search),
                              SizedBox(width: 10),
                              Text('搜索', style: TextStyle(color: Colors.grey)),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              // NestedScrollView 部分
              Expanded(
                child: NestedScrollView(
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
                    children: [
                      DiscoverRecommendWidget(),
                      FurnishFormWidget(),
                      DiscoverInformationWidget(),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
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
    return Container(
      color: Theme.of(context).scaffoldBackgroundColor, // 使用与页面相同的背景色
      child: tabBar,
    );
  }

  @override
  bool shouldRebuild(covariant _TabBarSliverDelegate oldDelegate) {
    return false;
  }
}

/// 自定义指示器，只显示文字宽度的下划线
class _CustomTabIndicator extends Decoration {
  final Color color;
  final double height;
  final TabController tabController;
  final double indicatorWeight;

  const _CustomTabIndicator({
    required this.color,
    required this.height,
    required this.tabController,
    required this.indicatorWeight,
  });

  @override
  BoxPainter createBoxPainter([VoidCallback? onChanged]) {
    return _CustomTabIndicatorPainter(
      color: color, 
      height: height, 
      tabController: tabController,
      indicatorWeight: indicatorWeight,
    );
  }
}

class _CustomTabIndicatorPainter extends BoxPainter {
  final Color color;
  final double height;
  final TabController tabController;
  final double indicatorWeight;

  _CustomTabIndicatorPainter({
    required this.color,
    required this.height,
    required this.tabController,
    required this.indicatorWeight,
  });

  @override
  void paint(Canvas canvas, Offset offset, ImageConfiguration configuration) {
    // 获取Tab的矩形区域
    final Rect rect = offset & configuration.size!;
    
    // 测量文本宽度
    final TextPainter textPainter = TextPainter(
      text: TextSpan(
        text: tabController.index < 3 ? 
              ['推荐', '装修', '资讯'][tabController.index] : '',
        style: const TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.bold,
        ),
      ),
      textDirection: TextDirection.ltr,
    );
    
    textPainter.layout();
    final double textWidth = textPainter.width;
    
    // 计算指示器位置
    final double tabWidth = rect.width;
    final double left = rect.left + (tabWidth - textWidth) / 2;
    final double bottom = rect.bottom - 2.0; // 距离底部的偏移
    
    // 绘制指示器
    final Paint paint = Paint()
      ..color = color
      ..style = PaintingStyle.fill;
      
    canvas.drawRect(
      Rect.fromLTWH(left, bottom, textWidth, indicatorWeight),
      paint,
    );
  }
}