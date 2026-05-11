# middlewares.py
import random

import requests
from scrapy import signals
from scrapy.downloadermiddlewares.httpproxy import HttpProxyMiddleware
from scrapy.downloadermiddlewares.useragent import UserAgentMiddleware


class KDLProxyMiddleware(HttpProxyMiddleware):
    def __init__(self, settings):
        super().__init__(settings)
        # 从settings读取快代理配置
        self.kdl_user = settings.get('KDL_USER')
        self.kdl_pass = settings.get('KDL_PASS')
        self.valid_proxies = []  # 有效代理池
        self.update_proxies()  # 初始化时获取代理

    def update_proxies(self):
        """调用快代理API，更新代理池"""
        try:
            res = requests.get(self.kdl_api, timeout=10).json()
            if res['code'] == 0:
                # 拼接代理格式：http://用户名:密码@IP:端口
                self.valid_proxies = [
                    f"http://{self.kdl_user}:{self.kdl_pass}@{ip}:{port}"
                    for ip, port in zip(res['data']['proxy_list'], res['data']['port_list'])
                ]
                print(f"成功获取{len(self.valid_proxies)}个快代理IP")
        except Exception as e:
            print(f"快代理API调用失败：{e}")

    def process_request(self, request, spider):
        # 代理池为空时，自动更新代理
        if not self.valid_proxies:
            self.update_proxies()

        # 随机选择一个代理
        proxy = random.choice(self.valid_proxies)
        request.meta['proxy'] = proxy
        spider.logger.info(f"当前使用快代理：{proxy.split('@')[-1]}")  # 日志隐藏账号密码

    def process_response(self, request, response, spider):
        # 遇到403/500等错误，移除当前代理并重试
        if response.status in [403, 500, 502]:
            proxy = request.meta.get('proxy')
            if proxy in self.valid_proxies:
                self.valid_proxies.remove(proxy)
                spider.logger.error(f"快代理{proxy.split('@')[-1]}失效，已移除")
            return request.copy()  # 重试请求
        return response

# 代理中间件（容错版）
class ProxyMiddleware(HttpProxyMiddleware):
    def __init__(self, settings):
        super().__init__(settings)
        self.proxies = settings.getlist('PROXIES')
        self.valid_proxies = self.proxies.copy()  # 有效代理列表


    @classmethod
    def from_crawler(cls, crawler):
        return cls(crawler.settings)

    def process_request(self, request, spider):
        # 若无有效代理，直接返回
        if not self.valid_proxies:
            spider.logger.warning('无可用代理，使用本机IP请求')
            return

        # 随机选择代理
        proxy = random.choice(self.valid_proxies)
        request.meta['proxy'] = proxy
        spider.logger.info(f'当前使用代理：{proxy}')

    def process_response(self, request, response, spider):
        # 若返回403/500等错误，标记代理失效并移除
        if response.status in [403, 407, 500, 502, 503]:
            proxy = request.meta.get('proxy')
            if proxy and proxy in self.valid_proxies:
                self.valid_proxies.remove(proxy)
                spider.logger.error(f'代理 {proxy} 失效，已移除（剩余可用：{len(self.valid_proxies)}）')
            # 重试请求（自动切换新代理）
            return request.copy()
        return response

    def process_exception(self, request, exception, spider):
        # 请求异常时，移除失效代理
        proxy = request.meta.get('proxy')
        if proxy and proxy in self.valid_proxies:
            self.valid_proxies.remove(proxy)
            spider.logger.error(f'代理 {proxy} 请求异常，已移除（剩余可用：{len(self.valid_proxies)}）')
        # 重试请求
        return request.copy()


# 随机User-Agent中间件
class RandomUserAgentMiddleware(UserAgentMiddleware):
    def __init__(self, settings):
        self.user_agents = settings.getlist('USER_AGENTS')

    @classmethod
    def from_crawler(cls, crawler):
        return cls(crawler.settings)

    def process_request(self, request, spider):
        # 随机选择User-Agent
        request.headers['User-Agent'] = random.choice(self.user_agents)