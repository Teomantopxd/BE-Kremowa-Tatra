import scrapy
import csv
import time
from scrapy.crawler import CrawlerProcess
from multiprocessing import Process

class ProductsSpider(scrapy.Spider):
    name = 'products'

    def __init__(self, file_path='', output_file_name='', *args, **kwargs):
        super(ProductsSpider, self).__init__(*args, **kwargs)
        self.start_urls = [file_path]
        self.output_filename = output_file_name

    def parse(self, response):
        file_exists = False
        try:
            with open(self.output_filename, 'r') as file:
                file_exists = True
        except FileNotFoundError:
            pass

        with open(self.output_filename, 'a', newline='', encoding='utf-8') as file:
            fieldnames = ['name', 'price', 'image_url', 'product_code', 'large_image_url', 'description']
            writer = csv.DictWriter(file, fieldnames=fieldnames)

            if not file_exists:
                writer.writeheader()

            for product in response.css('.item.AjaxBasket'):
                item = {
                    'name': product.css('.productName a span::text').get(),
                    'price': product.css('.price.priceGross::text').get(),
                    'image_url': product.css('.mainImage img::attr(src)').get()
                }

                product_page = product.css('.productText::attr(href)').get()
                if product_page:
                    yield response.follow(product_page, self.parse_product, meta={'item': item})

            next_page = response.css('li.next a.pageNext::attr(href)').get()
            if next_page:
                yield response.follow(next_page, self.parse)

    def parse_product(self, response):
        item = response.meta['item']
        item['product_code'] = response.css('.productCode.CodeJS .productCodeSwap::text').get()
        item['large_image_url'] = response.css('.duzaFotka::attr(src)').get()
        description = ' '.join(response.css('.con1.content span::text').getall())
        item['description'] = description.strip() if description else ''

        with open(self.output_filename, 'a', newline='', encoding='utf-8') as file:
            fieldnames = ['name', 'price', 'image_url', 'product_code', 'large_image_url', 'description']
            writer = csv.DictWriter(file, fieldnames=fieldnames)
            writer.writerow(item)


def run_spider(file_path, output_file_name):
    process = CrawlerProcess(settings={
        'FEED_FORMAT': 'csv',
        'FEED_URI': output_file_name,
        'LOG_LEVEL': 'DEBUG'  # Ustawienie logów na bardziej szczegółowe
    })
    process.crawl(ProductsSpider, file_path=file_path, output_file_name=output_file_name)
    process.start()

if __name__ == "__main__":
    start_time = time.time()
    processes = []
    tasks = [
        ('https://alkoholeswiata24.pl/whisky-online', 'scrap_results/whisky.csv'),
        ('https://alkoholeswiata24.pl/wodki-online', 'scrap_results/wodki.csv'),
        ('https://alkoholeswiata24.pl/rum-online', 'scrap_results/rum.csv'),
        ('https://alkoholeswiata24.pl/szampany', 'scrap_results/szampany.csv'),
        ('https://alkoholeswiata24.pl/wino-sklep', 'scrap_results/wino.csv'),
        ('https://alkoholeswiata24.pl/gin', 'scrap_results/gin.csv')
    ]

    for task in tasks:
        p = Process(target=run_spider, args=task)
        processes.append(p)
        p.start()

    for p in processes:
        p.join()

    total_time = time.time() - start_time
    print("Total:" + str(total_time))
