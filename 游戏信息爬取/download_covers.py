"""
下载豆瓣游戏封面图片，并更新 JSON 数据中的 cover 字段为本地路径
需要提前填入浏览器 Cookie（从 douban.com 复制，见下方说明）
"""
import json
import os
import time
import random
import urllib.parse

try:
    import requests
except ImportError:
    import subprocess, sys
    subprocess.check_call([sys.executable, "-m", "pip", "install", "requests"])
    import requests

INPUT_FILE = "游戏数据.txt"
OUTPUT_FILE = "游戏数据_本地封面.txt"
COVER_DIR = r"D:\GRecord\backend\src\main\resources\static\covers"
LOCAL_PATH_PREFIX = "/covers"

# ========================================================
# 【重要】将下面 COOKIE 替换成你浏览器里豆瓣的真实 Cookie
# 获取方法：
#   1. 浏览器打开 https://www.douban.com 并登录
#   2. F12 -> 网络(Network) -> 刷新页面 -> 点任意一个请求
#   3. 在请求头(Request Headers)里找到 "Cookie" 那一行，全部复制过来
# ========================================================
DOUBAN_COOKIE = 'll="108236"; bid=Diqktlfq4Qk; _pk_id.100001.8cb4=642cfb296cc055f0.1777306021.; _pk_ses.100001.8cb4=1; __utma=30149280.457191177.1777306025.1777306025.1777306025.1; __utmc=30149280; __utmz=30149280.1777306025.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); __utmt=1; ap_v=0,6.0; dbcl2="292936902:IXylAmFEi+c"; ck=O6Xb; frodotk_db="40089d685b42281ec019d83911c6785a"; push_noty_num=0; push_doumail_num=0; __utmv=30149280.29293; __utmb=30149280.6.10.1777306025'

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Accept": "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8",
    "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
    "Accept-Encoding": "gzip, deflate, br",
    "Referer": "https://www.douban.com/",
    "Cookie": DOUBAN_COOKIE,
    "Connection": "keep-alive",
    "Sec-Fetch-Dest": "image",
    "Sec-Fetch-Mode": "no-cors",
    "Sec-Fetch-Site": "cross-site",
}


def download_image(session: requests.Session, url: str, save_path: str) -> bool:
    try:
        resp = session.get(url, headers=HEADERS, timeout=15, stream=True)
        if resp.status_code == 200:
            with open(save_path, "wb") as f:
                for chunk in resp.iter_content(8192):
                    f.write(chunk)
            return True
        else:
            print(f"  [失败] HTTP {resp.status_code} -> {url}")
            return False
    except Exception as e:
        print(f"  [异常] {url} -> {e}")
        return False


def main():
    if not DOUBAN_COOKIE:
        print("=" * 60)
        print("请先填写豆瓣 Cookie！")
        print("步骤：")
        print("  1. 浏览器打开 https://www.douban.com 并登录")
        print("  2. F12 -> 网络(Network) -> 刷新页面")
        print("  3. 点任意一个 www.douban.com 的请求")
        print("  4. 在请求头里找到 Cookie 字段，全部复制")
        print("  5. 粘贴到脚本顶部 DOUBAN_COOKIE = \"\" 的引号内")
        print("=" * 60)
        return

    with open(INPUT_FILE, encoding="utf-8") as f:
        games = json.load(f)

    total = len(games)
    success = 0
    skip = 0
    fail = 0

    session = requests.Session()

    for i, game in enumerate(games, 1):
        cover_url = game.get("cover", "")
        if not cover_url or not cover_url.startswith("http"):
            skip += 1
            continue

        filename = os.path.basename(urllib.parse.urlparse(cover_url).path)
        if not filename:
            filename = f"cover_{i}.jpg"

        save_path = os.path.join(COVER_DIR, filename)
        local_url = f"{LOCAL_PATH_PREFIX}/{filename}"

        if os.path.exists(save_path) and os.path.getsize(save_path) > 0:
            game["cover"] = local_url
            skip += 1
            if i % 50 == 0:
                print(f"[{i}/{total}] 跳过(已存在): {filename}")
            continue

        print(f"[{i}/{total}] 下载: {filename}", end=" ... ", flush=True)
        ok = download_image(session, cover_url, save_path)
        if ok:
            game["cover"] = local_url
            success += 1
            print("OK")
        else:
            fail += 1

        # 随机间隔 0.5~1.5 秒，减少被封概率
        time.sleep(random.uniform(0.5, 1.5))

        # 每 100 张保存一次进度，防止中断丢失
        if i % 100 == 0:
            with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
                json.dump(games, f, ensure_ascii=False, indent=2)
            print(f"  >>> 已保存进度到 {OUTPUT_FILE}")

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(games, f, ensure_ascii=False, indent=2)

    print(f"\n===== 完成 =====")
    print(f"成功下载: {success}")
    print(f"跳过(已存在/无URL): {skip}")
    print(f"失败: {fail}")
    print(f"更新后的数据已保存至: {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
