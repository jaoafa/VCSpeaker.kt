import com.jaoafa.vcspeaker.tools.Twitter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull

class TwitterTest : FunSpec({
    test("getTweet") {
        val tweet = Twitter.getTweet("jaoafa", "1685568414084124673")

        tweet.shouldNotBeNull()
        tweet.authorName.shouldNotBeNull()
        tweet.html.shouldNotBeNull()
        tweet.plainText.shouldNotBeNull()
        tweet.readText.shouldNotBeNull()

        tweet.authorName shouldBeEqual "jao Community"
        tweet.html shouldBeEqual "<blockquote class=\"twitter-tweet\"><p lang=\"ja\" dir=\"ltr\">この度、jao Minecraft Serverでは2023年08月02日 22時00分をもって、Minecraft サーバのサービス提供を終了させていただくこととなりました。<br>利用者のみなさまには、突然のお知らせとなりますことをお詫びいたします。<br><br>7年間、本当にありがとうございました。<a href=\"https://twitter.com/hashtag/jaoafa?src=hash&amp;ref_src=twsrc%5Etfw\">#jaoafa</a></p>&mdash; jao Community (@jaoafa) <a href=\"https://twitter.com/jaoafa/status/1685568414084124673?ref_src=twsrc%5Etfw\">July 30, 2023</a></blockquote>\n<script async src=\"https://platform.twitter.com/widgets.js\" charset=\"utf-8\"></script>\n\n"
        tweet.plainText shouldBeEqual "この度、jao Minecraft Serverでは2023年08月02日 22時00分をもって、Minecraft サーバのサービス提供を終了させていただくこととなりました。\n利用者のみなさまには、突然のお知らせとなりますことをお詫びいたします。\n\n7年間、本当にありがとうございました。#jaoafa <https://twitter.com/hashtag/jaoafa?src=hash&ref_src=twsrc%5Etfw>"
        tweet.readText shouldBeEqual "この度、jao Minecraft Serverでは2023年08月02日 22時00分をもって、Minecraft サーバのサービス提供を終了させていただくこととなりました。\n利用者のみなさまには、突然のお知らせとなりますことをお詫びいたします。\n\n7年間、本当にありがとうございました。 ハッシュタグ「jaoafa」"
    }
})