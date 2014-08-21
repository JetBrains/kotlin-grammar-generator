<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    exclude-result-prefixes="#all"
    version="2.0">
    
    <xsl:output method="xml" encoding="UTF-8" indent="yes" omit-xml-declaration="no" cdata-section-elements="comment string doc"/>
    
    <xsl:template match="/">
        <xsl:variable name="firstPassTempTree">
            <tokens>
                <xsl:apply-templates/>
            </tokens>
        </xsl:variable>
        
        <xsl:variable name="secondPassTempTree">
            <tokens>
                <xsl:for-each-group select="$firstPassTempTree/tokens/*" group-by="@file-name">
                    <set file-name="{current-grouping-key()}">
                        <xsl:for-each select="current-group()">
                            <xsl:element name="{name()}">
                                <xsl:copy-of select="@*[name()!='file-name']|child::node()" copy-namespaces="no"/>
                            </xsl:element>
                        </xsl:for-each>
                    </set>
                </xsl:for-each-group>
            </tokens>
        </xsl:variable>
        
        <xsl:variable name="thirdPassTempTree">
            <tokens>
                <xsl:for-each select="$secondPassTempTree/tokens/set">
                    <set file-name="{@file-name}">
                        <xsl:choose>
                            <xsl:when test="child::declaration">
                                <xsl:variable name="total" select="count(child::declaration)"/>
                                <xsl:for-each select="child::declaration">
                                    <xsl:variable name="nextSlice">
                                        <slice>
                                            <xsl:copy-of select="following-sibling::*" copy-namespaces="no"/>
                                        </slice>
                                    </xsl:variable>
                                    <xsl:variable name="indexOfThis" select="position()"/>
                                    <item>
                                        <xsl:copy-of select="preceding-sibling::annotation[count(following-sibling::declaration)=$total - ($indexOfThis - 1)]" copy-namespaces="no"/>
                                        <xsl:copy-of select="." copy-namespaces="no"/>
                                        <description>
                                            <xsl:copy-of select="$nextSlice/slice/*[not(name()=('annotation','declaration','comment','doc')) 
                                                and count(./following-sibling::declaration)=$total - $indexOfThis]"/>
                                        </description>
                                    </item>
                                    <xsl:copy-of select="$nextSlice/slice/doc[count(./following-sibling::declaration)=$total - $indexOfThis]"/>
                                </xsl:for-each>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:copy-of select="child::node()" copy-namespaces="no"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </set> 
                </xsl:for-each>
            </tokens>
        </xsl:variable>
        
        <tokens>
            <xsl:for-each select="$thirdPassTempTree/tokens/set">
                <set file-name="{@file-name}">
                    <xsl:choose>
                        <xsl:when test="child::item">
                            <xsl:apply-templates mode="finalPass"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:copy-of select="child::node()" copy-namespaces="no"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </set>
            </xsl:for-each>
        </tokens>
    </xsl:template>
    
    <xsl:template match="*" mode="finalPass">
        <xsl:element name="{name(.)}">
            <xsl:copy-of select="@*" copy-namespaces="no"/>
            <xsl:apply-templates mode="finalPass"/>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="description" mode="finalPass">
        <description>
            <xsl:apply-templates mode="finalPass"/>
        </description>
    </xsl:template>
    
    <xsl:template match="crlf" mode="finalPass">
        <xsl:if test="not(name(following-sibling::*[1])='crlf') and 
            not(count(preceding-sibling::*)=0) and not(preceding-sibling::other[text()=';'])">
            <xsl:copy-of select="." copy-namespaces="no"/>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="token[@xsi:type='whiteSpace']">
        <xsl:variable name="atts" select="@file-name"/>
        <xsl:choose>
            <xsl:when test="contains(./text/text(),'&#x0a;')">
                <xsl:for-each select="tokenize(./text/text(),'&#x0a;')">
                    <xsl:if test="string-length(.)!=0">
                        <whiteSpace file-name="{$atts[1]}"><xsl:value-of select="."/></whiteSpace>
                    </xsl:if>
                    <xsl:if test="position()!=last()">
                        <crlf file-name="{$atts[1]}"/>
                    </xsl:if>
                </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
                <whiteSpace>
                    <xsl:copy-of select="@file-name" copy-namespaces="no"/>
                    <xsl:value-of select="./text/text()"/>
                </whiteSpace>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="token[@xsi:type=('docComment','comment')]">
        <xsl:element name="{if (matches(./text/text(),'[\n\r]')) then 'doc' else 'comment'}">
            <xsl:copy-of select="@file-name" copy-namespaces="no"/>
            <xsl:value-of select="replace(replace(replace(replace(replace(./text/text(),'/\*{1,2}',''),'\*/',''),'&amp;([a-z]+)','$1'),'^[ \s\t]+',''),'[ \s\t]+$','')"/>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="token[@xsi:type='stringToken']">
        <string>
            <xsl:copy-of select="@file-name" copy-namespaces="no"/>
            <xsl:value-of select="./text"/>
        </string>
    </xsl:template>
    
    <xsl:template match="token[@xsi:type='annotation']">
        <annotation>
            <xsl:copy-of select="@file-name" copy-namespaces="no"/>
            <xsl:value-of select="translate(./text,'[]','')"/>
        </annotation>
    </xsl:template>
    
    <xsl:template match="token[@xsi:type='symbolToken']">
        <symbol>
            <xsl:copy-of select="@file-name" copy-namespaces="no"/>
            <xsl:value-of select="./text"/>
        </symbol>
    </xsl:template>
    
    <xsl:template match="token[@xsi:type='declaration']">
        <declaration>
            <xsl:copy-of select="@file-name|@name|./usages" copy-namespaces="no"/>
        </declaration>
    </xsl:template>
    
    <xsl:template match="token[@xsi:type='identifier']">
        <identifier>
            <xsl:copy-of select="@file-name|@name|./usages" copy-namespaces="no"/>
        </identifier>
    </xsl:template>
    
    <xsl:template match="token">
        <xsl:element name="{@xsi:type}">
            <xsl:copy-of select="@file-name|@name" copy-namespaces="no"/>
            <xsl:value-of select="./text"/>
        </xsl:element>
    </xsl:template>
    
</xsl:stylesheet>