package io.fourfinanceit.pipeline.example.util

import groovy.xml.XmlUtil
import org.custommonkey.xmlunit.DetailedDiff
import org.custommonkey.xmlunit.Diff
import org.custommonkey.xmlunit.ElementNameAndAttributeQualifier
import org.custommonkey.xmlunit.XMLUnit
import org.junit.Before
import org.junit.ComparisonFailure

/**
 * @author Artur Gajowy
 * @author Marcin Grzejszczak
 */
//TODO package is as a separate test lib
//TODO rethink the way files are accessed in this class
trait XmlComparator {

    private XMLUnit xmlUnit

    @Before
    void init() {
        xmlUnit = new XMLUnit()
        xmlUnit.ignoreWhitespace = true
        xmlUnit.normalizeWhitespace = true
    }

    void compareXmls(String file, Node nodeToCompare) {
        //default parameter initializers are not allowed in traits
        compareXmls(file, nodeToCompare, false)
    }

    void compareXmls(String fileName, Node nodeToCompare, boolean displayActualXmlInCaseOfError) {
        String nodeXml = XmlUtil.serialize(nodeToCompare).stripIndent().stripMargin()
        def referenceXmlFile = getFileOrNull(fileName)
        if (!referenceXmlFile) {
            if (System.getProperty('outputMissingXml') == 'true') {
                def missingXml = new File("./src/test/resources/${fileName}")
                missingXml.parentFile.mkdirs()
                missingXml.text = nodeXml
            }
            throw new RuntimeException("Reference xml file [$fileName] not found")
        }
        String referenceXml = XmlUtil.serialize(referenceXmlFile.text).stripIndent().stripMargin()
        compareXmls(fileName, referenceXml, nodeXml, displayActualXmlInCaseOfError)
    }

    void compareXmls(String fileName, String referenceXml, String nodeXml, boolean displayActualXmlInCaseOfError) {
        Diff diff = xmlUnit.compareXML(referenceXml, nodeXml)
        diff.overrideElementQualifier(new ElementNameAndAttributeQualifier())
        if (!diff.identical()) {
            DetailedDiff detailedDiff = new DetailedDiff(diff)
            if (displayActualXmlInCaseOfError) {
                println("Actual XML:\n $nodeXml")
            }
            if (System.getProperty("outputActualXml") == 'true') {
                new File("src/test/resources/${fileName}.ACTUAL.xml").text = nodeXml
            }
            throw new XmlsAreNotSimilar(fileName, detailedDiff.allDifferences, referenceXml, nodeXml)
        }
    }

    private File getFileOrNull(String path) {
        URI uri = getClass()?.getResource(path)?.toURI()
        uri ? new File(uri) : null
    }

    static class XmlsAreNotSimilar extends ComparisonFailure {
        XmlsAreNotSimilar(String file, List diffs, String expected, String actual) {
            super("For file [$file] the following differences where found [$diffs].", expected, actual)
        }
    }
}
