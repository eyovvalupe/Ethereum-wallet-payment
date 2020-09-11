package io.horizontalsystems.bankwallet.modules.guideview

import org.commonmark.node.*
import java.net.URL

class GuideVisitorBlock(var guideUrl: String, val level: Int = 0, private val listItemMarkerGenerator: ListItemMarkerGenerator? = null) : AbstractVisitor() {

    val blocks = mutableListOf<GuideBlock>()
    private var quoted = false

    override fun visit(heading: Heading) {
        val content = GuideVisitorString.getNodeContent(heading, guideUrl)

        val block = when (heading.level) {
            1 -> GuideBlock.Heading1(content)
            2 -> GuideBlock.Heading2(content)
            3 -> GuideBlock.Heading3(content)
            else -> null
        }

        block?.let {
            blocks.add(block)
        }
    }

    override fun visit(paragraph: Paragraph) {
        val firstChild = paragraph.firstChild
        if (firstChild is Image && firstChild.next == null) {
            return visit(firstChild)
        }

        blocks.add(GuideBlock.Paragraph(GuideVisitorString.getNodeContent(paragraph, guideUrl), quoted))
    }

    override fun visit(image: Image) {
        val url = URL(URL(guideUrl), image.destination).toString()

        blocks.add(GuideBlock.Image(url, image.title, level == 0 && blocks.isEmpty()))
    }

    override fun visit(blockQuote: BlockQuote) {
        val guideVisitor = GuideVisitorBlock(guideUrl, level + 1)

        guideVisitor.visitChildren(blockQuote)

        guideVisitor.blocks.let { subblocks ->
            subblocks.forEach { it.quoted = true }
            subblocks.firstOrNull()?.quotedFirst = true
            subblocks.lastOrNull()?.quotedLast = true

            blocks.addAll(subblocks)
        }
    }

    override fun visit(bulletList: BulletList) {
        visitListBlock(bulletList, ListItemMarkerGenerator.Unordered)
    }

    override fun visit(orderedList: OrderedList) {
        visitListBlock(orderedList, ListItemMarkerGenerator.Ordered(orderedList.startNumber, orderedList.delimiter))
    }

    override fun visit(listItem: ListItem) {
        val guideVisitor = GuideVisitorBlock(guideUrl, level + 1)

        guideVisitor.visitChildren(listItem)
        guideVisitor.blocks.let { subblocks ->
            subblocks.forEach {
                it.listItem = true
            }
            subblocks.firstOrNull()?.listItemMarker = getNextListItemMarker()
            blocks.addAll(subblocks)
        }
    }

    private fun visitListBlock(listBlock: ListBlock, listItemMarkerGenerator: ListItemMarkerGenerator) {
        val guideVisitor = GuideVisitorBlock(guideUrl, level + 1, listItemMarkerGenerator)
        guideVisitor.visitChildren(listBlock)
        guideVisitor.blocks.let { subblocks ->
            if (listBlock.isTight) {
                subblocks.forEach {
                    it.listTightTop = true
                    it.listTightBottom = true
                }
                subblocks.firstOrNull()?.listTightTop = false
                subblocks.lastOrNull()?.listTightBottom = false
            }

            blocks.addAll(subblocks)
        }
    }

    private fun getNextListItemMarker(): String? {
        return listItemMarkerGenerator?.getNext()
    }
}
