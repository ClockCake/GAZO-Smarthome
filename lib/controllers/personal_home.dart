import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_smarthome/controllers/login_page.dart';
import 'package:flutter_smarthome/controllers/personal_order_segment.dart';
import 'package:flutter_smarthome/utils/hex_color.dart';
import 'package:flutter_smarthome/utils/network_image_helper.dart';
import 'package:oktoast/oktoast.dart';
import '../models/user_model.dart';
import '../utils/user_manager.dart';
import './personal_setting.dart';

class PersonalHomeWidget extends StatefulWidget {
  const PersonalHomeWidget({super.key});

  @override
  State<PersonalHomeWidget> createState() => _PersonalHomeWidgetState();
}

class _PersonalHomeWidgetState extends State<PersonalHomeWidget> {
  late bool isLogin;
  UserModel? user;
  
  // 订单标题数组
  final List<String> _orderTitles = [
    '待付款',
    '待发货', 
    '待收货',
    '待评价',
  ];
  
  // 订单图标数组
  final List<String> _orderIcons = [
    'assets/images/icon_order_nopay.png',
    'assets/images/icon_order_ship.png',
    'assets/images/icon_order_arrive.png',
    'assets/images/icon_order_evaluate.png',
  ];

  @override
  void initState() {
    super.initState();
    _updateLoginState();
    // 添加状态监听
    UserManager.instance.notifier.addListener(_updateLoginState);
  }

  @override
  void dispose() {
    // 移除状态监听
    UserManager.instance.notifier.removeListener(_updateLoginState);
    super.dispose();
  }

  // 更新登录状态
  void _updateLoginState() {
    setState(() {
      user = UserManager.instance.user;
      isLogin = UserManager.instance.isLoggedIn;
    });
  }

  // 设置按钮点击事件
  void _onSettingTap() {
    if (!isLogin) {
      Navigator.push(
        context,
        MaterialPageRoute(builder: (context) => LoginPage()),
      ).then((_) {
        // 登录页面返回后更新状态
        _updateLoginState();
      });
      return;
    } else {
      Navigator.push(
        context,
        MaterialPageRoute(builder: (context) => PersonalSettingWidget()),
      );
    }
  }

  // 用户名点击事件
  void _onUserNameTap() {
    if (!isLogin) {
      Navigator.push(
        context,
        MaterialPageRoute(builder: (context) => LoginPage()),
      ).then((_) {
        // 登录页面返回后更新状态
        _updateLoginState();
      });
    } else {
      // 暂无此业务逻辑
    }
  }

  @override
  Widget build(BuildContext context) {
    final topPadding = MediaQuery.of(context).padding.top;

    return Scaffold(
      body: SingleChildScrollView(
        child: Column(
          children: [
            _buildHeader(topPadding + 250.h),
            SizedBox(height: 16.h),
            _buildOrder(),
            _buildServiceCell(),
          ],
        ),
      ),
    );
  }

  // 构建头部
  Widget _buildHeader(double topPadding) {
    return Container(
      decoration: const BoxDecoration(
        image: DecorationImage(
          image: AssetImage('assets/images/icon_personal_bg.png'),
          fit: BoxFit.cover,
        ),
      ),
      width: double.infinity,
      height: topPadding,
      child: Column(
        children: [
          SizedBox(height: topPadding - 250.h),
          _buildTopBar(),
          _buildUserInfo(),
          SizedBox(height: 16.h),
          _buildFavoriteAndLike(),
          const Spacer(),
          _buildIntegralMall(),
        ],
      ),
    );
  }

  // 构建顶部设置按钮
  Widget _buildTopBar() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        IconButton(
          icon: Image.asset(
            'assets/images/icon_personal_setting.png',
            width: 24.w,
            height: 24.h,
          ),
          onPressed: _onSettingTap,
        ),
      ],
    );
  }

  // 构建用户信息
  Widget _buildUserInfo() {
    return Row(
      children: [
        SizedBox(width: 16.w),
        _buildAvatar(),
        SizedBox(width: 16.w),
        _buildUserName(),
      ],
    );
  }

  // 构建头像
  Widget _buildAvatar() {
    return ClipOval(
      child: Container(
        width: 48.w,
        height: 48.w,
        child: isLogin 
          ? NetworkImageHelper().getNetworkImage(
              imageUrl: (user?.avatar?.isEmpty ?? true) 
                  ? 'http://144.24.86.34:9002/i/2025/04/11/67f8c0c9e36d6.png' 
                  : user!.avatar!,
              width: 48.w,
              height: 48.w,
              fit: BoxFit.cover,
            )
          : Image.asset(
              'assets/images/icon_default_avatar.png',
              width: 48.w,
              height: 48.w,
              fit: BoxFit.cover,
            ),
      ),
    );
  }

  // 构建用户名
  Widget _buildUserName() {
    return InkWell(
      onTap: _onUserNameTap,
      child: Text(
        isLogin ? (user?.nickname ?? '用户名') : '登录/注册',
        style: TextStyle(
          color: Colors.black,
          fontSize: 16.sp,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  // 收藏/点赞
  Widget _buildFavoriteAndLike() {
    return Container(
      height: 44.h, // 增加固定高度以解决溢出问题
      child: Row(
        children: [
          Expanded(
            child: InkWell(
              onTap: () => showToast("暂未开放"),
              child: Container(
                alignment: Alignment.center,
                child: FittedBox( // 使用FittedBox确保内容适应容器
                  fit: BoxFit.scaleDown,
                  child: Column(
                    mainAxisSize: MainAxisSize.min, 
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        '0',
                        style: TextStyle(
                          fontSize: 14.sp, // 进一步减小字体大小
                          color: Colors.black,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      Text(
                        '收藏',
                        style: TextStyle(
                          fontSize: 10.sp,
                          color: HexColor('#999999'),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
          Container(
            width: 1,
            height: 24.h,
            color: Colors.grey[300],
          ),
          Expanded(
            child: InkWell(
              onTap: () => showToast("暂未开放"),
              child: Container(
                alignment: Alignment.center,
                child: FittedBox( // 使用FittedBox确保内容适应容器
                  fit: BoxFit.scaleDown,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        '0',
                        style: TextStyle(
                          fontSize: 14.sp, // 进一步减小字体大小
                          color: Colors.black,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      Text(
                        '点赞',
                        style: TextStyle(
                          fontSize: 10.sp,
                          color: Colors.grey,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  // 积分商城
  Widget _buildIntegralMall() {
    return Container(
      margin: EdgeInsets.only(top: 16.h, left: 16.w, right: 16.w),
      width: double.infinity,
      height: 54.h,
      decoration: const BoxDecoration(
        image: DecorationImage(
          image: AssetImage('assets/images/icon_score_bg.png'),
          fit: BoxFit.cover,
        ),
      ),
      child: Row(
        children: [
          SizedBox(width: 20.w),
          Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                '积分',
                style: TextStyle(
                  color: HexColor('#FFE6CF'),
                  fontSize: 12.sp,
                ),
              ),
              Text(
                '0',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 16.sp,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          const Spacer(),
          InkWell(
            onTap: () => showToast("暂未开放"),
            child: Container(
              width: 60.w,
              height: 28.h,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    HexColor('#FFE6CF'),
                    HexColor('#FFD0A5'),
                  ],
                ),
                borderRadius: BorderRadius.circular(14.r),
              ),
              child: Text(
                '签到',
                style: TextStyle(
                  color: HexColor('#433A34'),
                  fontSize: 12.sp,
                ),
              ),
            ),
          ),
          SizedBox(width: 20.w),
        ],
      ),
    );
  }

  // 我的订单
  Widget _buildOrder() {
    return Container(
      margin: EdgeInsets.only(top: 16.h),
      width: double.infinity,
      height: 100.h,
      child: Column(
        children: [
          Row(
            children: [
              SizedBox(width: 16.w),
              Text(
                '我的订单',
                style: TextStyle(
                  color: Colors.black,
                  fontSize: 15.sp,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const Spacer(),
              GestureDetector(
                onTap: () {
                  Navigator.push(
                    context, 
                    MaterialPageRoute(
                      builder: (context) => PersonalOrderSegmentWidget(index: 0)
                    )
                  );
                },
                child: Row(
                  children: [
                    Text(
                      '查看全部',
                      style: TextStyle(
                        color: HexColor('#999999'),
                        fontSize: 12.sp,
                      ),
                    ),
                    Icon(
                      Icons.arrow_forward_ios,
                      color: HexColor('#999999'),
                      size: 16.sp,
                    ),
                  ],
                ),
              ),
              SizedBox(width: 16.w),
            ],
          ),
          Expanded(
            child: Row(
              children: _orderTitles.asMap().keys.map((index) {
                return Expanded(
                  child: GestureDetector(
                    onTap: () {
                      Navigator.push(
                        context, 
                        MaterialPageRoute(
                          builder: (context) => PersonalOrderSegmentWidget(index: index + 1)
                        )
                      );
                    },
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Image.asset(
                          _orderIcons[index],
                          width: 24.w,
                          height: 24.h,
                        ),
                        SizedBox(height: 8.h),
                        Text(
                          _orderTitles[index],
                          style: TextStyle(
                            color: HexColor('#333333'),
                            fontSize: 12.sp,
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildServiceCell() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: EdgeInsets.fromLTRB(16.w, 16.h, 0, 0),
          child: Text(
            '我的服务',
            style: TextStyle(
              color: HexColor('#222222'),
              fontSize: 15.sp,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
        SizedBox(height: 16.h),
        _buildServiceItem('assets/images/icon_my_contract.png', '我的合同'),
        Divider(height: 1.h, color: HexColor('#E5E5E5')),
        _buildServiceItem('assets/images/icon_my_reserve.png', '我的预约'),
        Divider(height: 1.h, color: HexColor('#E5E5E5')),
        _buildServiceItem('assets/images/icon_my_comment.png', '我的评论'),
      ],
    );
  }

  Widget _buildServiceItem(String iconPath, String title) {
    return GestureDetector(
      onTap: () => showToast("暂未开放"),
      child: Row(
        children: [
          Container(
            width: 24.w,
            height: 24.h,
            margin: EdgeInsets.only(left: 16.w, top: 12.h, bottom: 12.h),
            child: Image.asset(iconPath),
          ),
          SizedBox(width: 8.w),
          Text(
            title,
            style: TextStyle(
              color: HexColor('#222222'),
              fontSize: 14.sp,
            ),
          ),
          const Spacer(),
          Icon(
            Icons.arrow_forward_ios,
            color: HexColor('#999999'),
            size: 16.sp,
          ),
          SizedBox(width: 16.w),
        ],
      ),
    );
  }
}
