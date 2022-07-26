<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="xml" indent="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="node()[not(self::comment())][not(self::*)]|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates select="@*">
                <xsl:sort select="name()"/>
            </xsl:apply-templates>
            <xsl:apply-templates>
                <xsl:sort select="name()"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="*:ArchiveDeliveryRequestReply">
        <xsl:element name="ArchiveTransfer" namespace="{namespace-uri()}">
            <xsl:apply-templates select="@*|node()"/>
        </xsl:element>
    </xsl:template>

    <xsl:variable name="BinaryDataObject">
        <xsl:copy-of select="/*:ArchiveTransfer/*:DataObjectPackage/*:BinaryDataObject"/>
    </xsl:variable>
    <xsl:variable name="PhysicalDataObject">
        <xsl:copy-of select="/*:ArchiveTransfer/*:DataObjectPackage/*:PhysicalDataObject"/>
    </xsl:variable>


    <xsl:template match="/*:ArchiveTransfer/*:DataObjectPackage">
        <xsl:copy>
            <xsl:apply-templates select="@*">
                <xsl:sort select="name()"/>
            </xsl:apply-templates>
            <xsl:apply-templates>
                <xsl:sort select="name()"/>
            </xsl:apply-templates>

            <xsl:for-each select="$BinaryDataObject">
                <xsl:variable name="nodeId" select="node()/*:DataObjectGroupId"/>
                <xsl:if test="$nodeId != ''">
                    <xsl:choose>
                        <xsl:when test="DataObjectGroup/@id = $nodeId">
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:element name="DataObjectGroup" namespace="{namespace-uri(node())}">
                                <xsl:attribute name="id" namespace="{namespace-uri()}">
                                    <xsl:value-of select="$nodeId"/>
                                </xsl:attribute>
                                <xsl:apply-templates select="current()">
                                    <xsl:sort select="name()"/>
                                </xsl:apply-templates>
                            </xsl:element>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
            </xsl:for-each>

            <xsl:for-each select="$PhysicalDataObject">
                <xsl:variable name="nodeId" select="node()/*:DataObjectGroupId"/>
                <xsl:if test="$nodeId != ''">
                    <xsl:choose>
                        <xsl:when test="DataObjectGroup/@id = $nodeId">
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:element name="DataObjectGroup" namespace="{namespace-uri(node())}">
                                <xsl:attribute name="id" namespace="{namespace-uri()}">
                                    <xsl:value-of select="$nodeId"/>
                                </xsl:attribute>
                                <xsl:apply-templates select="current()">
                                    <xsl:sort select="name()"/>
                                </xsl:apply-templates>
                            </xsl:element>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
            </xsl:for-each>

        </xsl:copy>
    </xsl:template>

    <xsl:template match="/*:ArchiveTransfer/*:DataObjectPackage/*:DataObjectGroup">
        <xsl:variable name="DataObjectGroupId" select="current()/@id"/>

        <xsl:copy>
            <xsl:apply-templates select="@*">
                <xsl:sort select="name()"/>
            </xsl:apply-templates>
            <xsl:apply-templates>
                <xsl:sort select="name()"/>
            </xsl:apply-templates>

            <xsl:for-each select="$BinaryDataObject">
                <xsl:variable name="nodeId" select="node()/*:DataObjectGroupReferenceId"/>
                <xsl:if test="$nodeId = $DataObjectGroupId">
                    <xsl:apply-templates select="current()">
                        <xsl:sort select="name()"/>
                    </xsl:apply-templates>
                </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="$PhysicalDataObject">
                <xsl:variable name="nodeId" select="node()/*:DataObjectGroupReferenceId"/>
                <xsl:if test="$nodeId = $DataObjectGroupId">
                    <xsl:apply-templates select="current()">
                        <xsl:sort select="name()"/>
                    </xsl:apply-templates>
                </xsl:if>
            </xsl:for-each>
        </xsl:copy>
    </xsl:template>


    <xsl:template match="*:ArchiveTransfer/*:DataObjectPackage/*:BinaryDataObject"/>

</xsl:stylesheet>
