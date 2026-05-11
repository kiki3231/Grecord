import json
import os


class GameScrapyPipeline:
    def open_spider(self, spider):
        # 确保文件路径正确
        self.file_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'douban_games.json')
        self.file = open(self.file_path, 'w', encoding='utf-8')
        self.file.write('[')
        self.first_item = True

    def process_item(self, item, spider):
        if not self.first_item:
            self.file.write(',')
        else:
            self.first_item = False

        # 转换为字典并写入
        item_dict = dict(item)
        line = json.dumps(item_dict, ensure_ascii=False, indent=2)
        self.file.write(line)
        return item

    def close_spider(self, spider):
        self.file.write(']')
        self.file.close()
        spider.logger.info(f'数据已保存到：{self.file_path}')