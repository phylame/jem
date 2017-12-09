/*
 * Copyright 2015-2017 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jem.format.pmab

import jem.epm.EpmFactory
import jem.epm.Maker
import jem.epm.PMAB_NAME
import jem.epm.Parser

internal object PMAB {
    /////** MIME type for PMAB **\\\\\
    const val MIME_PATH = "mimetype"
    const val MIME_PMAB = "application/pmab+zip"

    /////** PBM(PMAB Book Metadata) **\\\\\
    const val PBM_PATH = "book.xml"
    const val PBM_XMLNS = "http://phylame.pw/format/pmab/pbm"

    /////** PBC(PMAB Book Contents) **\\\\\
    const val PBC_PATH = "content.xml"
    const val PBC_XMLNS = "http://phylame.pw/format/pmab/pbc"
}

class PmabFactory : EpmFactory {
    override val keys = setOf(PMAB_NAME, "pem")

    override val name = "PMAB for Jem"

    override val hasMaker = true

    override val maker: Maker = PmabMaker

    override val hasParser = true

    override val parser: Parser = PmabParser
}
