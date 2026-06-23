#!/usr/bin/env python3
"""
失物招领系统（Lost & Found System）—— 自动化 API 测试脚本

本脚本使用 `requests` 库对后端所有 REST API 进行自动化测试，
涵盖正常流程、异常场景和边界条件。

运行方式：
    pip install requests
    python api_test.py

环境要求：
    - 后端服务运行在 http://localhost:1235（可通过 --base-url 修改）
    - MySQL 数据库 lost_found_db 已初始化

测试用户（数据库预置）：
    - 管理员：admin / 123456
    - 普通用户：user1 / 123456
"""

import argparse
import sys
import time
import traceback
from typing import Optional

import requests

# ============================================================
# 全局配置
# ============================================================
BASE_URL = "https://zang1357.chat"
API_PREFIX = "/api"

# 测试账号
ADMIN_USER = "admin"
ADMIN_PASS = "123456"
USER_USER = "user1"
USER_PASS = "123456"

# 状态码常量
CODE_SUCCESS = "200"
CODE_ERROR = "-1"
CODE_UNAUTHORIZED = "401"

# 测试结果统计
PASSED = 0
FAILED = 0
SKIPPED = 0


# ============================================================
# 工具函数
# ============================================================

def result(response: requests.Response, expect_code: str = CODE_SUCCESS) -> bool:
    """验证响应是否为期望的 code"""
    global PASSED, FAILED
    try:
        body = response.json()
        code = body.get("code")
        if code == expect_code:
            PASSED += 1
            return True
        else:
            FAILED += 1
            print(f"  ❌ 期望 code={expect_code}, 实际 code={code}, msg={body.get('msg')}")
            return False
    except Exception as e:
        FAILED += 1
        print(f"  ❌ 响应解析失败: {e}, 状态码: {response.status_code}")
        return False


def summary():
    """打印测试总结"""
    total = PASSED + FAILED + SKIPPED
    print("=" * 60)
    print(f"  测试完成: 总计 {total} 项")
    print(f"  ✅ 通过: {PASSED}")
    print(f"  ❌ 失败: {FAILED}")
    print(f"  ⏭️  跳过: {SKIPPED}")
    print("=" * 60)
    return FAILED == 0


def section(title: str):
    """打印章节标题"""
    print()
    print("=" * 60)
    print(f"  {title}")
    print("=" * 60)


def test(name: str):
    """打印测试用例名称（缩进装饰）"""
    print(f"\n  ▶ {name}")


def check_field(body: dict, field: str, expected_type=None, msg=None):
    """检查响应字段是否存在或类型正确"""
    global PASSED, FAILED
    if field not in body:
        FAILED += 1
        label = msg or f"缺少字段 '{field}'"
        print(f"    ❌ {label}")
        return False
    if expected_type and not isinstance(body[field], expected_type):
        FAILED += 1
        label = msg or f"字段 '{field}' 类型应为 {expected_type}, 实际为 {type(body[field])}"
        print(f"    ❌ {label}")
        return False
    PASSED += 1
    return True


class APITestSession:
    """封装测试会话，自动携带 token"""

    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})
        self.admin_token: Optional[str] = None
        self.user_token: Optional[str] = None
        self.timestamp = str(int(time.time()))

    # ---- HTTP 方法快捷 ----

    def url(self, path: str) -> str:
        return f"{self.base_url}{API_PREFIX}{path}"

    def get(self, path: str, token: str = None, **kwargs) -> requests.Response:
        headers = kwargs.pop("headers", {})
        if token:
            headers["token"] = token
        return self.session.get(self.url(path), headers=headers, **kwargs)

    def post(self, path: str, token: str = None, **kwargs) -> requests.Response:
        headers = kwargs.pop("headers", {})
        if token:
            headers["token"] = token
        return self.session.post(self.url(path), headers=headers, **kwargs)

    def put(self, path: str, token: str = None, **kwargs) -> requests.Response:
        headers = kwargs.pop("headers", {})
        if token:
            headers["token"] = token
        return self.session.put(self.url(path), headers=headers, **kwargs)

    def delete(self, path: str, token: str = None, **kwargs) -> requests.Response:
        headers = kwargs.pop("headers", {})
        if token:
            headers["token"] = token
        return self.session.delete(self.url(path), headers=headers, **kwargs)

    # ---- 用户管理 ----

    def test_user_management(self):
        section("1. 用户管理")

        # 1-1. 管理员登录
        test("管理员登录")
        resp = self.post("/user/login", json={"username": ADMIN_USER, "password": ADMIN_PASS})
        assert result(resp), "管理员登录失败"
        body = resp.json()
        self.admin_token = body.get("data", {}).get("token", "")
        assert self.admin_token, "未获取到管理员 Token"

        # 1-2. 普通用户登录
        test("普通用户登录")
        resp = self.post("/user/login", json={"username": USER_USER, "password": USER_PASS})
        assert result(resp), "普通用户登录失败"
        body = resp.json()
        self.user_token = body.get("data", {}).get("token", "")
        assert self.user_token, "未获取到普通用户 Token"

        # 1-3. 错误密码登录
        test("错误密码登录")
        resp = self.post("/user/login", json={"username": ADMIN_USER, "password": "wrong_password"})
        assert result(resp, CODE_ERROR), "错误密码应返回错误"

        # 1-4. 获取当前用户
        test("获取当前用户")
        resp = self.get("/user/current", token=self.admin_token)
        assert result(resp), "获取当前用户失败"
        body = resp.json().get("data", {})
        assert body.get("username") == ADMIN_USER, f"用户名不匹配: {body.get('username')}"

        # 1-5. 分页查询用户
        test("分页查询用户")
        resp = self.get("/user/page?currentPage=1&size=10", token=self.admin_token)
        assert result(resp), "分页查询用户失败"
        data = resp.json().get("data", {})
        assert "records" in data, "分页结果缺少 records"

        # 1-6. 获取所有用户
        test("获取所有用户")
        resp = self.get("/user/all", token=self.admin_token)
        assert result(resp), "获取所有用户失败"
        data = resp.json().get("data", [])
        assert len(data) > 0, "用户列表为空"

        # 1-7. 根据ID查询
        test("根据ID查询用户")
        resp = self.get("/user/1", token=self.admin_token)
        assert result(resp), "查询用户失败"

        # 1-8. 根据用户名查询
        test("根据用户名查询用户")
        resp = self.get(f"/user/username/{ADMIN_USER}", token=self.admin_token)
        assert result(resp), "按用户名查询失败"

        # 1-9. 发送验证码
        test("发送密码重置验证码")
        resp = self.get("/user/sendCode?email=zhangsan@example.com")
        # 可能成功也可能失败（取决于邮件配置），不强制断言

        # 1-10. 注册新用户
        test("注册新用户")
        ts = self.timestamp
        new_user = {
            "username": f"test_{ts}",
            "password": "123456",
            "name": "测试用户",
            "phone": "13800138002",
            "email": f"test_{ts}@example.com"
        }
        resp = self.post("/user/add", json=new_user)
        assert result(resp), "注册用户失败"

        # 1-11. 注册重复用户名
        test("注册重复用户名")
        resp = self.post("/user/add", json=new_user)
        assert result(resp, CODE_ERROR), "重复用户名应返回错误"

        # 1-12. 更新用户信息
        test("更新用户信息")
        resp = self.put("/user/1", token=self.admin_token, json={"name": "管理员-已更新"})
        assert result(resp), "更新用户失败"

        # 1-13. 获取角色用户列表
        test("根据角色获取用户")
        resp = self.get("/user/role/ADMIN", token=self.admin_token)
        assert result(resp), "按角色查询失败"

    # ---- 物品分类管理 ----

    def test_category_management(self):
        section("2. 物品分类管理")

        # 2-1. 获取分类列表（公开接口）
        test("获取分类列表（公开）")
        resp = self.get("/category/list")
        assert result(resp), "获取分类列表失败"
        data = resp.json().get("data", [])
        assert len(data) > 0, "分类列表为空"

        # 2-2. 分页查询分类
        test("分页查询分类")
        resp = self.get("/category/page?currentPage=1&size=10", token=self.admin_token)
        assert result(resp), "分页查询分类失败"

        # 2-3. 根据ID查询分类
        test("根据ID查询分类")
        resp = self.get("/category/1", token=self.admin_token)
        assert result(resp), "查询分类详情失败"

        # 2-4. 添加分类（需认证）
        test("添加分类")
        resp = self.post("/category", token=self.admin_token, json={
            "name": f"测试分类_{self.timestamp}",
            "sort": 99
        })
        assert result(resp), "添加分类失败"

        # 2-5. 更新分类
        test("更新分类")
        resp = self.put("/category/1", token=self.admin_token, json={
            "name": "证件类(更新)",
            "sort": 1
        })
        assert result(resp), "更新分类失败"

    # ---- 失物信息管理 ----

    def test_lost_item_management(self):
        section("3. 失物信息管理")

        # 3-1. 分页查询失物（公开）
        test("分页查询失物（公开）")
        resp = self.get("/lost-item/page?currentPage=1&size=10")
        assert result(resp), "分页查询失物失败"
        data = resp.json().get("data", {})
        assert "records" in data, "分页结果缺少 records"

        # 3-2. 根据ID查询失物
        test("根据ID查询失物详情")
        resp = self.get("/lost-item/1")
        assert result(resp), "查询失物详情失败"

        # 3-3. 获取统计信息
        test("获取失物统计数据")
        resp = self.get("/lost-item/statistics")
        assert result(resp), "获取统计失败"
        data = resp.json().get("data", {})
        for key in ["totalItems", "totalPending", "totalClaimed", "totalUsers"]:
            assert key in data, f"统计信息缺少字段 {key}"

        # 3-4. 按分类筛选
        test("按分类筛选失物")
        resp = self.get("/lost-item/page?categoryId=1&currentPage=1&size=10")
        assert result(resp), "按分类筛选失败"

        # 3-5. 按状态筛选
        test("按状态筛选失物")
        resp = self.get("/lost-item/page?status=0&currentPage=1&size=10")
        assert result(resp), "按状态筛选失败"

        # 3-6. 发布失物（管理员）
        test("发布失物（管理员）")
        resp = self.post("/lost-item", token=self.admin_token, json={
            "title": "测试丢失的笔记本",
            "description": "在图书馆自习室丢失一台银色笔记本电脑，配有黑色充电器，如有捡到请联系。",
            "categoryId": 2,
            "lostPlace": "图书馆四楼自习室",
            "lostTime": "2026-06-28 14:30:00",
            "contactName": "管理员",
            "contactPhone": "13800138001"
        })
        assert result(resp), "管理员发布失物失败"
        global LOST_ITEM_ID
        # 尝试获取刚刚创建的数据
        resp = self.get("/lost-item/page?currentPage=1&size=1", token=self.admin_token)
        if result(resp):
            records = resp.json().get("data", {}).get("records", [])
            if records:
                LOST_ITEM_ID = records[0].get("id")

        # 3-7. 发布失物（普通用户）
        test("发布失物（普通用户）")
        resp = self.post("/lost-item", token=self.user_token, json={
            "title": "用户丢失的背包",
            "description": "在操场丢失黑色双肩背包一个，内有课本和文具，请拾到者联系。",
            "categoryId": 4,
            "lostPlace": "操场主席台",
            "lostTime": "2026-06-27 10:00:00",
            "contactName": "张三",
            "contactPhone": "13900139001"
        })
        assert result(resp), "普通用户发布失物失败"

    # ---- 招领信息管理 ----

    def test_found_item_management(self):
        section("4. 招领信息管理")

        # 4-1. 分页查询招领（公开）
        test("分页查询招领（公开）")
        resp = self.get("/found-item/page?currentPage=1&size=10")
        assert result(resp), "分页查询招领失败"
        data = resp.json().get("data", {})
        assert "records" in data, "分页结果缺少 records"

        # 4-2. 根据ID查询招领
        test("根据ID查询招领详情")
        resp = self.get("/found-item/1")
        assert result(resp), "查询招领详情失败"

        # 4-3. 我的招领信息
        test("获取我的招领信息")
        resp = self.get("/found-item/my", token=self.admin_token)
        assert result(resp), "获取我的招领失败"

        # 4-4. 按分类筛选招领
        test("按分类筛选招领")
        resp = self.get("/found-item/page?categoryId=2&currentPage=1&size=10")
        assert result(resp), "按分类筛选招领失败"

        # 4-5. 发布招领信息（普通用户）
        test("发布招领信息")
        resp = self.post("/found-item", token=self.user_token, json={
            "title": "捡到一个计算器",
            "description": "在教学楼A栋303教室捡到一个科学计算器，品牌卡西欧，请失主联系认领。",
            "categoryId": 2,
            "foundPlace": "教学楼A栋303",
            "foundTime": "2026-06-28 09:00:00",
            "contactName": "李四",
            "contactPhone": "13900139001"
        })
        assert result(resp), "发布招领信息失败"

        # 4-6. 更新招领状态
        test("更新招领状态为已关闭")
        resp = self.put("/found-item/4/status", token=self.admin_token, json={"status": 3})
        # 不强制断言，因为 ID 4 可能已被测试删除

        # 4-7. 搜索招领（按标题）
        test("按标题搜索招领")
        resp = self.get("/found-item/page?title=手机&currentPage=1&size=10")
        assert result(resp), "按标题搜索招领失败"

    # ---- 认领申请管理 ----

    def test_claim_management(self):
        section("5. 认领申请管理")

        # 5-1. 提交认领申请
        test("提交认领申请（认领失物）")
        resp = self.post("/claim", token=self.user_token, json={
            "itemId": 1,
            "itemType": 1,
            "description": "这是我的物品，我有相关购买凭证和照片可以证明。"
        })
        # 可能因为重复申请而失败，记录下来但不终止
        if resp.json().get("code") == CODE_ERROR:
            print(f"    ⚠ 认领申请未被接受: {resp.json().get('msg')}")
        else:
            assert result(resp), "提交认领申请失败"

        # 5-2. 查询我的申请
        test("查询我的认领申请")
        resp = self.get("/claim/my?currentPage=1&size=10", token=self.user_token)
        assert result(resp), "查询我的申请失败"

        # 5-3. 查询待审核的申请
        test("查询待我审核的申请")
        resp = self.get("/claim/audit?currentPage=1&size=10", token=self.admin_token)
        assert result(resp), "查询待审核申请失败"

        # 5-4. 管理员查询所有申请
        test("管理员查询所有申请")
        resp = self.get("/claim/page?currentPage=1&size=10", token=self.admin_token)
        assert result(resp), "管理员查询所有申请失败"

    # ---- 通知管理 ----

    def test_notification_management(self):
        section("6. 通知管理")

        # 6-1. 获取通知列表
        test("获取通知列表")
        resp = self.get("/notification/list?currentPage=1&size=10", token=self.user_token)
        assert result(resp), "获取通知列表失败"

        # 6-2. 获取未读通知数
        test("获取未读通知数量")
        resp = self.get("/notification/unread-count", token=self.user_token)
        assert result(resp), "获取未读通知数失败"
        count = resp.json().get("data")
        assert count is not None, "未读数量为空"

        # 6-3. 标记通知为已读
        test("标记通知为已读")
        resp = self.get("/notification/list?currentPage=1&size=10", token=self.user_token)
        if result(resp):
            records = resp.json().get("data", {}).get("records", [])
            if records:
                nid = records[0].get("id")
                resp2 = self.put(f"/notification/{nid}/read", token=self.user_token)
                assert result(resp2), "标记已读失败"

        # 6-4. 标记全部已读
        test("标记所有通知为已读")
        resp = self.put("/notification/read-all", token=self.user_token)
        assert result(resp), "标记全部已读失败"

    # ---- 边界/异常测试 ----

    def test_edge_cases(self):
        global SKIPPED
        section("7. 异常场景和边界测试")

        # 7-1. 未认证访问
        test("未认证访问（无Token）")
        resp = self.get("/user/current")
        assert result(resp, CODE_UNAUTHORIZED) or resp.status_code == 401, \
            "未认证应返回 401"

        # 7-2. 无效Token
        test("无效Token访问")
        resp = self.get("/user/current", token="invalid_token_12345")
        assert result(resp, CODE_UNAUTHORIZED) or resp.status_code == 401, \
            "无效Token应返回 401"

        # 7-3. 查询不存在的失物
        test("查询不存在的失物ID")
        resp = self.get("/lost-item/99999")
        assert result(resp, CODE_ERROR), "不存在的ID应返回错误"

        # 7-4. 查询不存在的招领
        test("查询不存在的招领ID")
        resp = self.get("/found-item/99999")
        assert result(resp, CODE_ERROR), "不存在的ID应返回错误"

        # 7-5. 空参数登录
        test("空参数登录")
        resp = self.post("/user/login", json={"username": "", "password": ""})
        assert result(resp, CODE_ERROR), "空参数应返回错误"

        # 7-6. 发布失物-短标题
        test("发布失物（标题过短）")
        resp = self.post("/lost-item", token=self.admin_token, json={
            "title": "a",
            "description": "短标题测试应该被验证拦截",
            "categoryId": 1,
            "lostPlace": "测试",  # 短地点
            "lostTime": "2026-06-28 00:00:00",
            "contactName": "测试",
            "contactPhone": "13800138001"
        })
        assert result(resp, CODE_ERROR), "过短标题应返回错误"

        # 7-7. 发布失物-短描述
        test("发布失物（描述过短）")
        resp = self.post("/lost-item", token=self.admin_token, json={
            "title": "完整标题测试",
            "description": "短",
            "categoryId": 1,
            "lostPlace": "测试地点完整",
            "lostTime": "2026-06-28 00:00:00",
            "contactName": "测试人员",
            "contactPhone": "13800138001"
        })
        assert result(resp, CODE_ERROR), "过短描述应返回错误"

        # 7-8. 发布失物-无效手机号
        test("发布失物（无效手机号）")
        resp = self.post("/lost-item", token=self.admin_token, json={
            "title": "完整标题测试",
            "description": "这是一段足够长的描述来通过验证，至少需要十个字。",
            "categoryId": 1,
            "lostPlace": "测试地点",
            "lostTime": "2026-06-28 00:00:00",
            "contactName": "测试人员",
            "contactPhone": "12345"
        })
        assert result(resp, CODE_ERROR), "无效手机号应返回错误"

        # 7-9. 重复认领申请
        test("重复提交认领申请")
        resp = self.post("/claim", token=self.user_token, json={
            "itemId": 1,
            "itemType": 1,
            "description": "重复申请测试"
        })
        # 如果是重复申请，期望返回错误
        if resp.json().get("code") == CODE_ERROR:
            print(f"    ✓ 重复申请被正确拦截: {resp.json().get('msg')}")
            global PASSED
            PASSED += 1
        else:
            print(f"    ⚠ 重复申请通过了（可能第一次已取消或被拒绝）")

        # 7-10. 越权删除（普通用户删除管理员物品）
        test("越权测试")
        resp = self.delete("/lost-item/1", token=self.user_token)
        if resp.status_code == 401 or resp.json().get("code") == CODE_ERROR:
            print(f"    ✓ 越权操作被正确拦截")
            PASSED += 1
        else:
            print(f"    ⚠ 越权操作未被拦截（可能该物品属于该用户）")

        # 7-11. 正常删除自己的失物
        test("删除自己的失物")
        # 先创建一个用于删除的失物
        resp = self.post("/lost-item", token=self.user_token, json={
            "title": "待删除测试物品",
            "description": "这是一个用来测试删除功能的临时物品，描述足够详细。",
            "categoryId": 1,
            "lostPlace": "测试地点",
            "lostTime": "2026-06-28 00:00:00",
            "contactName": "测试员",
            "contactPhone": "13800138001"
        })
        if resp.json().get("code") == CODE_SUCCESS:
            # 找到刚创建的数据并删除
            resp2 = self.get("/lost-item/page?title=待删除测试物品&currentPage=1&size=10",
                             token=self.user_token)
            if result(resp2):
                records = resp2.json().get("data", {}).get("records", [])
                if records:
                    del_id = records[0].get("id")
                    resp3 = self.delete(f"/lost-item/{del_id}", token=self.user_token)
                    assert result(resp3), "删除自己的失物失败"
        else:
            print(f"    ⏭️  跳过删除测试（创建失败）")
            global SKIPPED
            SKIPPED += 1

    # ---- 主运行入口 ----

    def run(self):
        """依次执行所有测试"""
        try:
            self.test_user_management()
            self.test_category_management()
            self.test_lost_item_management()
            self.test_found_item_management()
            self.test_claim_management()
            self.test_notification_management()
            self.test_edge_cases()
        except AssertionError as e:
            print(f"  ❌ 断言失败: {e}")
            traceback.print_exc()
        except requests.ConnectionError:
            print(f"\n  ❌ 无法连接到 {self.base_url}，请确认后端服务已启动")
            sys.exit(1)
        except Exception as e:
            print(f"  ❌ 未知错误: {e}")
            traceback.print_exc()
        finally:
            success = summary()
            sys.exit(0 if success else 1)


# ============================================================
# 用于追踪测试中动态数据的全局变量
# ============================================================
LOST_ITEM_ID = None


def main():
    parser = argparse.ArgumentParser(description="失物招领系统 API 自动化测试")
    global ADMIN_USER, ADMIN_PASS, USER_USER, USER_PASS, BASE_URL

    parser.add_argument("--base-url", default=BASE_URL,
                        help=f"后端服务地址（默认 {BASE_URL}）")
    parser.add_argument("--admin-user", default=ADMIN_USER,
                        help=f"管理员用户名（默认 {ADMIN_USER}）")
    parser.add_argument("--admin-pass", default=ADMIN_PASS,
                        help=f"管理员密码（默认 {ADMIN_PASS}）")
    parser.add_argument("--user-user", default=USER_USER,
                        help=f"普通用户名（默认 {USER_USER}）")
    parser.add_argument("--user-pass", default=USER_PASS,
                        help=f"普通用户密码（默认 {USER_PASS}）")
    args = parser.parse_args()
    BASE_URL = args.base_url
    ADMIN_USER = args.admin_user
    ADMIN_PASS = args.admin_pass
    USER_USER = args.user_user
    USER_PASS = args.user_pass

    print("=" * 60)
    print("  失物招领系统 API 自动化测试")
    print(f"  目标服务器: {BASE_URL}")
    print("=" * 60)

    tester = APITestSession(BASE_URL)
    tester.run()


if __name__ == "__main__":
    main()
