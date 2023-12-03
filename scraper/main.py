import scrapy
import csv
import re
import time
from scrapy.crawler import CrawlerProcess
from multiprocessing import Process

class ProductsSpider(scrapy.Spider):
    name = 'products'

    def __init__(self, file_path='', output_file_name='', category='', *args, **kwargs):
        super(ProductsSpider, self).__init__(*args, **kwargs)
        self.start_urls = [file_path]
        self.output_filename = output_file_name
        self.category = category
        self.tax = 1

    def parse(self, response):
        file_exists = False
        try:
            with open(self.output_filename, 'r') as file:
                file_exists = True
        except FileNotFoundError:
            pass

        with open(self.output_filename, 'a', newline='', encoding='utf-8') as file:
            fieldnames = ['name', 'price', 'image_url', 'quantity', 'description', 'category', 'tax']
            writer = csv.DictWriter(file, fieldnames=fieldnames, delimiter=';')

            if not file_exists:
                writer.writeheader()

            for product in response.css('.item.AjaxBasket'):
                item = {
                    'name': product.css('.productName a span::text').get(),
                    'price': product.css('.price.priceGross::text').get(),
                    'image_url': 'https://alkoholeswiata24.pl/' + product.css('.mainImage img::attr(src)').get(),
                    'category': self.category,
                    'tax': self.tax
                }

                product_page = product.css('.productText::attr(href)').get()
                if product_page:
                    yield response.follow(product_page, self.parse_product, meta={'item': item})

            next_page = response.css('li.next a.pageNext::attr(href)').get()
            if next_page:
                yield response.follow(next_page, self.parse)

    def parse_product(self, response):
        item = response.meta['item']
        # Zmiana product code na quantity
        quantity_text = response.css('.productDetails.availability .value::text').get()
        quantity = re.search(r'\d+', quantity_text)
        raw_quantity = int(quantity.group()) if quantity else 0  # Konwersja na liczbę całkowitą, jeśli jest, w przeciwnym razie 0
        item['quantity'] = min(raw_quantity, 10)

        large_image_url = 'https://alkoholeswiata24.pl/' + response.css('.duzaFotka::attr(src)').get()
        item['image_url'] = f"{item['image_url']}, {large_image_url}" if item.get('image_url') else large_image_url

        description = ' '.join(response.css('.con1.content span::text').getall())
        item['description'] = description.strip() if description else ''

        item['name'] = item['name'].replace('#', ' ')

        with open(self.output_filename, 'a', newline='', encoding='utf-8') as file:
            fieldnames = ['name', 'price', 'image_url', 'quantity', 'description', 'category', 'tax']
            writer = csv.DictWriter(file, fieldnames=fieldnames, delimiter=';')
            writer.writerow(item)


def run_spider(file_path, output_file_name, category):
    process = CrawlerProcess(settings={
        'FEED_FORMAT': 'csv',
        'FEED_URI': output_file_name,
        'LOG_LEVEL': 'DEBUG'  # Ustawienie logów na bardziej szczegółowe
    })
    process.crawl(ProductsSpider, file_path=file_path, output_file_name=output_file_name, category=category)
    process.start()


if __name__ == "__main__":
    start_time = time.time()
    processes = []
    tasks = [
        ('https://alkoholeswiata24.pl/whiskey-irlandzka', 'scrap_results/whisky.csv', 'whisky zachodnie'),
        ('https://alkoholeswiata24.pl/whisky-japonska', 'scrap_results/whisky.csv', 'whisky wschodnie'),
        ('https://alkoholeswiata24.pl/whiskey-amerykanska', 'scrap_results/whisky.csv', 'whisky zachodnie'),
        ('https://alkoholeswiata24.pl/whisky-indyjska,272,0.html', 'scrap_results/whisky.csv', 'whisky wschodnie'),
        ('https://alkoholeswiata24.pl/whisky-szkocka', 'scrap_results/whisky.csv', 'whisky zachodnie'),
        ('https://alkoholeswiata24.pl/wodki-smakowe', 'scrap_results/wodki.csv', 'wodki smakowe'),
        ('https://alkoholeswiata24.pl/wodki-owocowe', 'scrap_results/wodki.csv', 'wodki owocowe'),
        ('https://alkoholeswiata24.pl/szampan-armand-de-brignac', 'scrap_results/szampany.csv', 'armand de brignac'),
        ('https://alkoholeswiata24.pl/szampan-dom-perignon', 'scrap_results/szampany.csv', 'dom perigon'),
        ('https://alkoholeswiata24.pl/wino-czerwone', 'scrap_results/wino.csv', 'wino czerwone'),
        ('https://alkoholeswiata24.pl/wino-biale', 'scrap_results/wino.csv', 'wino białe')
    ]
    for task in tasks:
        p = Process(target=run_spider, args=task)
        processes.append(p)
        p.start()

    for p in processes:
        p.join()

    total_time = time.time() - start_time
    print("Total:" + str(total_time))