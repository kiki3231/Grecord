import requests
import pandas as pd
from time import sleep
import numpy as np

# 你的Steam API Key
STEAM_API_KEY = "124492A296C007D24029B56298D77F18"

# 1. 获取所有游戏列表
def get_all_games():
    url = "http://api.steampowered.com/ISteamApps/GetAppList/v2/"
    try:
        response = requests.get(url)
        response.raise_for_status()
        apps = response.json()['applist']['apps']
        # 转换为DataFrame，并过滤掉非游戏项（初步过滤，后续还需详细检查）
        all_games_df = pd.DataFrame(apps)
        # 注意：这里获取的列表包含所有类型的App，需要进一步筛选
        return all_games_df
    except requests.exceptions.RequestException as e:
        print(f"获取游戏列表失败: {e}")
        return pd.DataFrame()

# 2. 获取游戏详情并过滤出近半年发布的游戏
def get_game_details(appid):
    """获取单个游戏的详细信息，包括发行日期"""
    url = f"https://store.steampowered.com/api/appdetails?appids={appid}"
    try:
        response = requests.get(url)
        response.raise_for_status()
        data = response.json()
        if data[str(appid)]['success']:
            game_data = data[str(appid)]['data']
            # 检查类型是否为游戏
            if game_data.get('type') != 'game':
                return None
            release_date = game_data.get('release_date', {}).get('date', '')
            # 返回需要的游戏信息，如名称、发行日期、类型、是否免费等
            return {
                'appid': appid,
                'name': game_data.get('name'),
                'release_date': release_date,
                # ... 其他需要的信息
            }
        return None
    except requests.exceptions.RequestException as e:
        print(f"获取游戏 {appid} 详情失败: {e}")
        return None
    sleep(1.1)  # 控制请求频率，避免过快

# 3. 获取游戏的全球成就解锁百分比
def get_global_achievements(appid):
    """获取游戏的全球成就解锁百分比"""
    if not appid:
        return None
    url = f"http://api.steampowered.com/ISteamUserStats/GetGlobalAchievementPercentagesForApp/v0002/?gameid={appid}"
    try:
        response = requests.get(url)
        response.raise_for_status()
        data = response.json()
        achievements = data['achievementpercentages']['achievements']
        return achievements
    except requests.exceptions.RequestException as e:
        # 很多游戏没有成就系统或数据不可用，失败是常见的
        # print(f"获取游戏 {appid} 成就数据失败: {e}")
        return None
    sleep(1.1)

# 4. 估算玩家数量（基于成就）
def estimate_players_from_achievements(achievements):
    """通过成就数据估算玩家数量（非常粗略的估算）"""
    if not achievements:
        return None
    # 找到最简单的那个成就（假设解锁率最高的那个是第一个或最简单的）
    simplest_achievement = max(achievements, key=lambda x: x['percent'])
    highest_percent = simplest_achievement['percent']
    # 一个非常粗略的假设：解锁最简单成就的玩家比例应该接近100%，如果有5%的玩家没解锁，说明玩家基数可能更大。
    # 这是一个极其简化的模型，实际模型要复杂得多。
    # 例如：假设解锁率95%意味着玩家数量是 visible_player_base / 0.95
    # 但这里的 visible_player_base 是多少呢？Steam不公开。
    # 此处仅为示例，需要更复杂的研究和校准。
    # 可能需要找一个已知大致销量的游戏作为基准进行反推。
    estimated_players = 100000 / (highest_percent / 100)  # 这里的100000是一个任意选择的基准数
    return estimated_players

# 5. 获取游戏的好评数
def get_review_count(appid):
    """获取游戏的好评数量"""
    url = f"https://store.steampowered.com/appreviews/{appid}?json=1"
    try:
        response = requests.get(url)
        response.raise_for_status()
        data = response.json()
        if data['success'] == 1:
            total_reviews = data['query_summary']['total_reviews']
            return total_reviews
        return 0
    except requests.exceptions.RequestException as e:
        print(f"获取游戏 {appid} 评测数据失败: {e}")
        return 0
    sleep(1.1)

# 6. 主逻辑流程
def main():
    # 获取所有游戏
    print("获取游戏列表...")
    all_games_df = get_all_games()
    if all_games_df.empty:
        print("未获取到游戏列表。")
        return

    game_list_for_analysis = []
    # 遍历游戏列表（这里应该只处理一部分，比如前1000个，或者先筛选出近半年的游戏ID）
    # 注意：Steam游戏数量庞大，全部处理非常耗时，需要优化策略，例如先获取近期游戏ID。
    for index, row in all_games_df.head(100).iterrows():  # 示例只处理前100个
        appid = row['appid']
        name = row['name']

        print(f"处理游戏: {name} ({appid})")

        # 获取游戏详情，并筛选出近半年发布的游戏
        details = get_game_details(appid)
        if not details:
            continue

        # 检查发行日期是否在近半年内 (此处需要实现日期逻辑)
        # release_date = details['release_date']
        # if not is_recent_six_months(release_date): # 需要实现is_recent_six_months函数
        #     continue

        # 获取成就数据
        achievements = get_global_achievements(appid)
        # 估算玩家数量
        estimated_players = estimate_players_from_achievements(achievements)
        # 获取好评数
        review_count = get_review_count(appid)

        game_info = {
            'appid': appid,
            'name': name,
            'release_date': details['release_date'],
            'estimated_players': estimated_players,
            'review_count': review_count,
            # ... 其他信息
        }
        game_list_for_analysis.append(game_info)
        sleep(1.1)  # 礼貌性暂停，避免请求过快

    # 转换为DataFrame
    results_df = pd.DataFrame(game_list_for_analysis)
    # 清理数据，去除estimated_players为NaN或0的行
    results_df = results_df.dropna(subset=['estimated_players'])
    results_df = results_df[results_df['estimated_players'] > 0]

    # 计算综合热度分数（示例，需要归一化）
    # 归一化估算玩家数
    max_players = results_df['estimated_players'].max()
    min_players = results_df['estimated_players'].min()
    results_df['norm_players'] = (results_df['estimated_players'] - min_players) / (max_players - min_players)

    max_reviews = results_df['review_count'].max()
    min_reviews = results_df['review_count'].min()
    results_df['norm_reviews'] = (results_df['review_count'] - min_reviews) / (max_reviews - min_reviews) if max_reviews > min_reviews else 0

    results_df['composite_score'] = (results_df['norm_players'] * 0.5) + (results_df['norm_reviews'] * 0.3) # 这里只用了两个指标

    # 按综合分数降序排列
    sorted_df = results_df.sort_values(by='composite_score', ascending=False)
    # 获取Top 50
    top_50_games = sorted_df.head(50)
    print(top_50_games[['name', 'appid', 'release_date', 'estimated_players', 'review_count', 'composite_score']])
    # 保存结果到CSV
    top_50_games.to_csv('top_50_estimated_games_past_six_months.csv', index=False)

if __name__ == "__main__":
    main()