package org.specs2
package html

import scala.xml._
import NodeSeq._
import transform.{RewriteRule, RuleTransformer}
import xml.Nodex._
import Htmlx._
import data.Trees._
import org.specs2.internal.scalaz.Scalaz
import Scalaz._
import specification.SpecName

/**
 * This trait checks for the presence of a <toc/> tag at the beginning of a xml document and replaces it
 * by a list of links to the headers of the document
 */
private[specs2]
trait TableOfContents { outer =>

  /**
   * Create a Table of contents by building a Tree of all the header elements contained into the "body" and mapping it to an <ul/> list
   *
   * The body can contain <subtoc id="something"/> elements showing where to insert sub-table of contents corresponding to linked documents
   *
   * @param body html where to extract the table of contents
   * @param url of the document linked to the root of the toc
   * @param id of the specification that is the root of the toc (it is used to "open" the toc list on the current specification).
   *        This id is generated by the specification SpecName
   * @param subTocs map of identifier -> toc for the sub-specifications. The subtocs are inserted where the <subtoc/> tag in present in the body
   *
   * @return the toc of a document
   */
  def tocItemList(body: NodeSeq, url: String, id: SpecId, subTocs: Map[SpecId, NodeSeq]): NodeSeq = {
    body.headersTree.
      bottomUp { (h: Header, s: Stream[NodeSeq]) =>
        if (h.isRoot)
		  // 'id' is the name of the attribute expected by jstree to "open" the tree on a specific node
          s.reduceNodes.updateHeadAttribute("id", id)
        else if (h.isSubtoc)
          subTocs.get(h.specId).getOrElse(Empty) ++ s.reduceNodes
        else
          <li id={h.specId}><a href={h.anchorName(url)}>{h.name}</a>
            { <ul>{s.toSeq}</ul> unless (s.toSeq.isEmpty) }
          </li>
    }.rootLabel
  }
}

private[specs2]
object TableOfContents extends TableOfContents
