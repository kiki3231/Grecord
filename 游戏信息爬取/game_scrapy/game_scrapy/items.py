# items.py
import scrapy
class GameScrapyItem(scrapy.Item):
    cover = scrapy.Field() #游戏封面
    name = scrapy.Field()   #游戏名称
    rating = scrapy.Field() #游戏评分
    platform = scrapy.Field()  #游戏平台
    review = scrapy.Field()  # 最佳评论
    intro = scrapy.Field()   # 简介
    game_type = scrapy.Field()  # 游戏类型