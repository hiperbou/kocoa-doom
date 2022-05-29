package rr

import p.Resettable

/**
 *
 * A SubSector. References a Sector. Basically, this is a list of LineSegs,
 * indicating the visible walls that define (all or some) sides of a convex BSP
 * leaf.
 *
 * @author admin
 */
class subsector_t @JvmOverloads constructor(// Maes: single pointer
    var sector: sector_t? = null, // e6y: support for extended nodes
    // 'int' instead of 'short'
    var numlines: Int = 0, var firstline: Int = 0
) : Resettable {
    override fun toString(): String {
        subsector_t.sb.setLength(0)
        subsector_t.sb.append("Subsector")
        subsector_t.sb.append('\t')
        subsector_t.sb.append("Sector: ")
        subsector_t.sb.append(sector)
        subsector_t.sb.append('\t')
        subsector_t.sb.append("numlines ")
        subsector_t.sb.append(numlines)
        subsector_t.sb.append('\t')
        subsector_t.sb.append("firstline ")
        subsector_t.sb.append(firstline)
        return subsector_t.sb.toString()
    }

    override fun reset() {
        sector = null
        numlines = 0
        firstline = numlines
    }

    companion object {
        private val sb = StringBuilder()
    }
}