package proj.biznes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.*;

public class SeleniumTest
{
    WebDriver driver;

    @BeforeEach
    public void setup()
    {
        driver = new ChromeDriver();
        driver.get("https://localhost/index.php");
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    @Test
    public void mainTest()
    {
        //ignore protection
        driver.findElement(By.id("details-button")).click();
        driver.findElement(By.id("proceed-link")).click();

        //a. Dodanie do koszyka 10 produktów (w różnych ilościach) z dwóch różnych kategorii,
        goToCategory("WHISKY");
        Map<String, Integer> whisky = new HashMap<>(
                Map.of("whiskey-bourbon-angel-s-envy-port-wine-barrels-finish-433-07l", 3,
                        "whisky-paul-john-single-malt-christmas-edition-2022-07l-46-indie", 1,
                        "whiskey-bushmills-10yo-07l-40-szkl-irlandia", 2,
                        "whisky-hatozaki-pure-malt-07l-46-japonia", 1,
                        "whiskey-bourbon-gold-bar-black-double-cask-075l-46", 1));
        addToCartFromCurrentPage(whisky);
        goToCategory("WÓDKI");
        Map<String, Integer> wodki = new HashMap<>(
                Map.of("likier-adam-mickiewicz-gorzka-pomarancza-05l-30", 2,
                        "wino-365-blackberry-cz-ps-075l-12-karafka", 1,
                        "likier-debowa-cherry-gold-07l-30-12-czekoladowych-kieliszkow", 3,
                        "gin-wolf-oak-rzemieslnicza-gruszkowa-07l-38-40", 1,
                        "grappa-sessantanni-di-primitivo-invecchiata-40-05l", 1));
        addToCartFromCurrentPage(wodki);

        //b. Wyszukanie produktu po nazwie i dodanie do koszyka losowego produktu spośród znalezionych
        addRandomItemToCartFromSearch("wino aris solaris", 1);

        //c. Usunięcie z koszyka 3 produktów,
        removeItemsFromCart(3);

        //d. Rejestrację nowego konta,
        registerNewAccount();

        //e. Wykonanie zamówienia zawartości koszyka,
        placeOrder();

        //(f)g. Wybór jednego z dwóch przewoźników,
        chooseDelivery();

        //(g)f. Wybór metody płatności: przy odbiorze,
        choosePayment();

        //h. Zatwierdzenie zamówienia,
        confirmOrder();

        //i. Sprawdzenie statusu zamówienia.
        checkOrderStatus();

        //j. Pobranie faktury VAT.
        downloadInvoice();

        driver.findElement(By.cssSelector("a[rel=\"nofollow\"]")).click();
        driver.quit();
    }

    private static boolean isFileDownloaded(String downloadPath)
    {
        File dir = new File(downloadPath);
        File[] dirContents = dir.listFiles();
        if (dirContents != null)
        {
            for (File file : dirContents)
            {
                if (file.getName().endsWith(".pdf"))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private void downloadInvoice()
    {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(2000));
        driver.findElement(By.cssSelector("a[href*='pdf-invoice']")).click();
        String downloadPath = System.getProperty("user.home") + "\\Downloads";
        wait.until((WebDriver d) -> isFileDownloaded(downloadPath));
    }

    private void checkOrderStatus()
    {
        driver.findElement(By.className("account")).findElement(By.className("hidden-sm-down")).click();
        driver.findElement(By.id("history-link")).findElement(By.className("link-item")).click();
        driver.findElement(By.cssSelector("[data-link-action='view-order-details']")).click();
    }

    private void confirmOrder()
    {
        driver.findElement(By.className("condition-label")).findElement(By.className("js-terms")).click();
        driver.findElement(By.id("payment-confirmation")).findElement(By.className("btn-primary")).click();
    }

    private void choosePayment()
    {
        driver.findElement(By.id("payment-option-3-container")).findElement(By.className("custom-radio")).click();
    }

    private void chooseDelivery()
    {
        driver.findElement(By.className("delivery-option")).click();
        driver.findElement(By.name("confirmDeliveryOption")).click();
    }

    private void placeOrder()
    {
        driver.findElement(By.className("shopping-cart")).click();
        driver.findElement(By.className("btn-primary")).click();
        driver.findElement(By.id("field-address1")).sendKeys("27 Lambda Street");
        driver.findElement(By.id("field-postcode")).sendKeys("12-345");
        driver.findElement(By.id("field-city")).sendKeys("City 17");
        driver.findElement(By.name("confirm-addresses")).click();
    }

    private void registerNewAccount()
    {
        driver.findElement(By.cssSelector("a[rel=\"nofollow\"]")).click();
        driver.findElement(By.className("no-account")).click();
        driver.findElement(By.id("field-firstname")).sendKeys("Gordon");
        driver.findElement(By.id("field-lastname")).sendKeys("Freeman");
        driver.findElement(By.id("field-email")).sendKeys("gfreeman@black.mesa");
        driver.findElement(By.id("field-password")).sendKeys("gmansucks");
        driver.findElement(By.name("customer_privacy")).click();
        driver.findElement(By.name("psgdpr")).click();
        driver.findElement(By.className("form-control-submit")).click();
    }

    private void removeItemsFromCart(int amount)
    {
        driver.findElement(By.className("shopping-cart")).click();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(2000));
        for (int i=0; i<amount; i++)
        {
            WebElement removeButton = driver.findElement(By.className("remove-from-cart"));
            removeButton.click();
            wait.until(ExpectedConditions.stalenessOf(removeButton));
        }
    }

    private void addRandomItemToCartFromSearch(String item, int amount)
    {
        searchForItem(item);
        List<WebElement> products = driver.findElements(By.className("product-title"));
        Random rand = new Random();
        int randIndex = rand.nextInt(products.size());
        products.get(randIndex).click();
        addToCartCurrentItem(amount);
    }

    private void searchForItem(String item)
    {
        driver.findElement(By.className("ui-autocomplete-input"))
                .sendKeys(Keys.CONTROL+"a", Keys.BACK_SPACE);
        driver.findElement(By.className("ui-autocomplete-input"))
                .sendKeys(item + Keys.ENTER);
    }

    private void goToCategory(String page)
    {
        new Actions(driver).moveToElement(driver.findElement(By.id("top-menu"))).perform();
        List<WebElement> elementName = driver.findElements(By.className("category"));
        Optional<WebElement> randomElement = elementName.stream().filter(e -> e.getText().equals(page)).findFirst();
        randomElement.ifPresent(WebElement::click);
    }

    private void addToCartFromCurrentPage(Map<String, Integer> itemsAmount) {
        itemsAmount.forEach((item, amount) -> {
            driver.findElements(By.className("product-title"))
                    .stream()
                    .filter(e -> e.findElement(By.tagName("a")).getAttribute("href").contains(item))
                    .findFirst()
                    .ifPresent(WebElement::click);
            addToCartCurrentItem(amount);
            driver.navigate().back();
        });
    }

    private void addToCartCurrentItem(int amount)
    {
        driver.findElement(By.id("quantity_wanted"))
                .sendKeys(Keys.CONTROL+"a", Keys.BACK_SPACE);
        driver.findElement(By.id("quantity_wanted"))
                .sendKeys(String.valueOf(amount));
        driver.findElement(By.className("add-to-cart")).click();
        driver.findElement(By.className("cart-content-btn")).findElement(By.className("btn-secondary")).click();
    }
}
