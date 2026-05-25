import scrapy
import json
import sys
import os

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from items import GameScrapyItem


class DoubanGameSpider(scrapy.Spider):
    name = 'douban_game'
    allowed_domains = ['douban.com']
    start_urls = ['https://www.douban.com/game/explore']
    # ========== 核心修改：设置起始页码 ==========
    current_page = 700
    is_has_more = True

    def start_requests(self):
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36',
            'Cookie': 'bid=29FAHSd09_A; _pk_id.100001.8cb4=9e6405e73ba07531.1767076867.; push_noty_num=0; push_doumail_num=0; dbcl2="292936902:X2qo6yh7b7Y"; ck=Vi1k; ap_v=0,6.0; _pk_ref.100001.8cb4=%5B%22%22%2C%22%22%2C1768046305%2C%22https%3A%2F%2Fcn.bing.com%2F%22%5D; _pk_ses.100001.8cb4=1; frodotk_db="06df643c50f7cb9f149e2cb4f2e762dd"',
            'Referer': 'https://www.douban.com/game/explore'
        }
        # ========== 核心修改：直接发起指定页的接口请求 ==========
        if self.is_has_more:
            api_url = f'https://www.douban.com/j/ilmen/game/search?genres=&platforms=&q=&sort=rating&more={self.current_page}'
            yield scrapy.Request(
                url=api_url,
                headers={
                    'User-Agent': headers['User-Agent'],
                    'Cookie': headers['Cookie'],
                    'Referer': 'https://www.douban.com/game/explore',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                callback=self.parse_more_api,
                meta={'page_num': self.current_page},
                priority=80
            )

    def parse_more_api(self, response):
        current_page = response.meta['page_num']
        try:
            data = json.loads(response.text)
            games = data.get('games', [])
            self.logger.info(f'===== 第 {current_page} 页接口找到 {len(games)} 个游戏 =====')

            if len(games) == 0:
                self.is_has_more = False
                self.logger.info(f'===== 第 {current_page} 页无数据，爬取结束 =====')
                return

            for game in games:
                detail_url = game.get('url', '').strip()
                if not detail_url:
                    continue

                item = GameScrapyItem()
                item['cover'] = game.get('cover', '').strip().replace('_s.jpg', '_l.jpg')
                item['name'] = game.get('title', '').strip()
                item['rating'] = game.get('rating', 0.0)
                platform_str = game.get('platforms', '').replace('/', '').replace(' ', '')
                item['platform'] = self.clean_platform(platform_str)
                item['game_type'] = game.get('genres', '').strip().replace('游戏 / ', '')
                item['review'] = game.get('review', {}).get('content', '').strip()
                item['intro'] = ''

                yield scrapy.Request(
                    url=detail_url,
                    headers=response.request.headers,
                    callback=self.parse_detail_page,
                    meta={'item': item},
                    priority=70
                )

            self.current_page += 1
            next_api_url = f'https://www.douban.com/j/ilmen/game/search?genres=&platforms=&q=&sort=rating&more={self.current_page}'
            yield scrapy.Request(
                url=next_api_url,
                headers={
                    'User-Agent': response.request.headers['User-Agent'],
                    'Cookie': response.request.headers['Cookie'],
                    'Referer': 'https://www.douban.com/game/explore',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                callback=self.parse_more_api,
                meta={'page_num': self.current_page},
                priority=60
            )

        except json.JSONDecodeError:
            self.logger.error(f'第 {current_page} 页接口JSON解析失败，终止爬取：{response.text[:300]}')
            self.is_has_more = False
        except Exception as e:
            self.logger.error(f'第 {current_page} 页解析出错，终止爬取：{str(e)}')
            self.is_has_more = False

    def parse_detail_page(self, response):
        item = response.meta['item']
        intro_nodes = response.xpath('//div[contains(@class, "mod item-desc")]/p//text()')
        if intro_nodes:
            item['intro'] = ''.join([text.strip() for text in intro_nodes.extract() if text.strip()])
        else:
            item['intro'] = '无简介'

        self.logger.info(f'''
============================================
【爬取完成】{item["name"]}
评分：{item["rating"]} | 平台：{item["platform"]}
============================================''')
        yield item

    def clean_platform(self, platform_str):
        if not platform_str:
            return "未知平台"

        platform_str = platform_str.replace('/', '').replace(' ', '').strip()
        platform_mapping = {
            'PC': 'PC', 'Mac': 'Mac', 'iPhone': 'iPhone', 'Android': 'Android',
            'PS4': 'PS4', 'PS5': 'PS5', 'NintendoSwitch2': 'Nintendo Switch 2',
            'NintendoSwitch': 'Nintendo Switch', 'SteamDeck': 'Steam Deck',
            'XboxOne': 'Xbox One', 'iPad': 'iPad', 'WiiU': 'Wii U', 'PS3': 'PS3',
            'Xbox360': 'Xbox 360', 'Wii': 'Wii', 'PS2': 'PS2', 'Xbox': 'Xbox',
            'NGCGameCube': 'NGC/GameCube', 'PS': 'PS', 'PSVPSVita': 'PSV/PS Vita',
            'PSP': 'PSP', '3DS': '3DS', 'NDS': 'NDS',
            'Arcade街机': 'Arcade/街机', 'FCNES红白机': 'FC/NES/红白机',
            'SFCSNES超任': 'SFC/SNES/超任', 'MD世嘉五代': 'MD/世嘉五代',
            'N64任天堂64': 'N64/任天堂64', 'GB': 'GB', 'GBA': 'GBA',
            'DCDreamcast': 'DC/Dreamcast', 'SS世嘉土星': 'SS/世嘉土星',
            'Linux': 'Linux', 'WindowsPhone': 'Windows Phone', '3DO': '3DO',
            'PCEngine': 'PC-Engine', 'WonderSwan': 'WonderSwan',
            'NeoGeo': 'Neo Geo', 'NeoGeoPocket': 'Neo Geo Pocket',
            'Atari2600': 'Atari 2600', '浏览器': '浏览器', '文曲星': '文曲星',
            'SteamVR': 'Steam VR', 'ValveIndex': 'Valve Index',
            'HTCVive': 'HTC Vive', 'OculusRift': 'Oculus Rift',
            'OculusQuest': 'Oculus Quest', 'OculusGo': 'Oculus Go',
            'PlayStationVR': 'PlayStation VR'
        }

        cleaned_platforms = []
        max_len = len(platform_str)
        for length in range(max_len, 0, -1):
            for i in range(max_len - length + 1):
                candidate = platform_str[i:i + length]
                if candidate in platform_mapping:
                    cleaned_platforms.append(platform_mapping[candidate])
                    platform_str = platform_str[:i] + platform_str[i + length:]
                    max_len = len(platform_str)
                    break
            if not platform_str:
                break

        return '、'.join(list(set(cleaned_platforms))) if cleaned_platforms else "未知平台"