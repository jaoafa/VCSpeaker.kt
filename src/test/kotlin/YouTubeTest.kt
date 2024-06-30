
import com.jaoafa.vcspeaker.tools.YouTube
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class YouTubeTest : FunSpec({
    test("If the video ID is valid, the video information should be returned.") {
        val video = YouTube.getVideo("aEbZ1UXcKhI")

        video.shouldNotBeNull()
        video.title shouldBe "とまち式! わかりやすい! BIGみそか"
        video.authorName shouldBe "jaotan"
    }

    test("If the playlist ID is valid, the playlist information should be returned.") {
        val playlist = YouTube.getPlaylist("PL98tDGwe38KzTLvjPA0FJEqHl2y-_QufX")

        playlist.shouldNotBeNull()
        playlist.title shouldBe "1.mp4"
        playlist.authorName shouldBe "jaotan"
    }
})