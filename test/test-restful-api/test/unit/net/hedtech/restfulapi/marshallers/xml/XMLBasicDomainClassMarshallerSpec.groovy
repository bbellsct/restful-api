/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.marshallers.xml

import grails.test.mixin.*
import grails.test.mixin.web.*
import spock.lang.*
import net.hedtech.restfulapi.*
import grails.converters.XML
import grails.test.mixin.support.*
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.springframework.web.context.WebApplicationContext
import org.springframework.beans.BeanWrapper
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import grails.test.mixin.domain.DomainClassUnitTestMixin

import org.junit.Rule
import org.junit.rules.TestName

@TestMixin([GrailsUnitTestMixin, ControllerUnitTestMixin,DomainClassUnitTestMixin])
@Mock([MarshalledThing,MarshalledPartOfThing,
       MarshalledSubPartOfThing,MarshalledThingContributor,
       MarshalledOwnerOfThing,MarshalledThingEmbeddedPart])
class XMLBasicDomainClassMarshallerSpec extends Specification {

    @Rule TestName testName = new TestName()

    void setup() {
    }

    def "Test with Id"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.includeIdFor << {Object o -> true }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.save()
        thing.id = 1

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        '1'  == xml.id.text()
        'AA' == xml.code.text()
        1    == xml.description.size()
    }

    def "Test without Id"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.includeIdFor << {Object o -> false }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.id = 1

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0    == xml.id.size()
        'AA' == xml.code.text()
    }

    def "Test with version"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.includeVersionFor << {Object o -> true }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.id = 1
        thing.version = 1

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        '1'  == xml.version.text()
        'AA' == xml.code.text()
    }

    def "Test without version"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.includeVersionFor << {Object o -> false }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.id = 1
        thing.version = 1

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0    == xml.version.size()
        'AA' == xml.code.text()
    }

    def "Test excluding fields"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.getExcludedFields << {Object value-> ['description','isLarge'] }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0    == xml.description.size()
        0    == xml.isLarge.size()
        'AA' == xml.code.text()
    }

    def "Test default exclusions"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", lastModifiedBy:'John Smith' )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0    == xml.lastModifiedBy.size()
        null != thing.lastModifiedBy

    }


    def "Test including fields"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.getIncludedFields << {Object value-> [ 'code', 'description','parts'] }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        'AA'       == xml.code.text()
        'aa thing' == xml.description.text()
        0 == xml.contributors.size()
        0 == xml.embeddedPart.size()
        0 == xml.owner.size()
        0 == xml.subPart.size()
        0 == xml.isLarge.size()
        0 == xml.lastModified.size()
        0 == xml.lastModifiedBy.size()
        0 == xml.dataOrigin.size()
    }

    def "Test that included fields overrides excluded fields"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.getIncludedFields << {Object value-> [ 'code', 'description','lastModifiedBy'] }
        marshaller.metaClass.getExcludedFields << {Object value->
            exclusionCalled = true
            [ 'code', 'description']
        }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", lastModifiedBy:'John Smith' )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        'AA'         == xml.code.text()
        'aa thing'   == xml.description.text()
        'John Smith' == xml.lastModifiedBy.text()

    }

    def "Test special processing of simple fields"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.processField << {
            BeanWrapper beanWrapper, GrailsDomainClassProperty property, XML xml ->
            if (property.getName() == 'description') {
                xml.startNode('modDescription')
                xml.convertAnother(beanWrapper.getPropertyValue(property.getName()))
                xml.end()
                return false
            } else {
                return true
            }
        }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0          == xml.description.size()
        'aa thing' == xml.modDescription.text()
        'AA'       == xml.code.text()
    }

    def "Test alternative name of simple field"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.getSubstitutionName << {
            BeanWrapper beanWrapper, GrailsDomainClassProperty property ->
            if (property.getName() == 'description') {
                return 'modDescription'
            } else {
                return null
            }
        }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0          == xml.description.size()
        'aa thing' == xml.modDescription.text()
        'AA'       == xml.code.text()
    }

    def "Test that null Collection association field is marshalled as an empty node"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.parts = null

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        null   == thing.parts
        1      == xml.parts.size()
        0      == xml.parts.children().size()
        "true" == xml.parts.'@array'.text()
    }

    def "Test that empty Collection association field is marshalled as an empty node"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.parts = []

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        []     == thing.parts
        1      == xml.parts.size()
        0      == xml.parts.children().size()
        "true" == xml.parts.'@array'.text()
    }

    def "Test that null Map association field is marshalled as an empty node"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", contributors:null )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        null   == thing.contributors
        1      == xml.contributors.size()
        0      == xml.contributors.children().size()
        "true" == xml.contributors.'@map'.text()
    }


    def "Test that empty Map association field is marshalled as an empty node"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", contributors:[:] )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        [:]    == thing.contributors
        1      == xml.contributors.size()
        0      == xml.contributors.children().size()
        "true" == xml.contributors.'@map'.text()
    }

    def "Test that null one-to-one association field is marshalled as an empty node"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", subPart:null )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        null == thing.subPart
        1    == xml.subPart.size()
        0    == xml.subPart.children().size()

    }

    def "Test that null many-to-one association field is marshalled as an empty node"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", owner:null )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        null == thing.owner
        1    == xml.owner.size()
        0    == xml.owner.children().size()
    }

    def "Test that null embedded association field is marshalled as an empty node"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", embeddedPart:null )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        null == thing.embeddedPart
        1    == xml.embeddedPart.size()
        0    == xml.embeddedPart.children().size()
    }

    def "Test that associated collection carries the array attribute"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        def parts = []
        def part = new MarshalledPartOfThing(code:'partA',desc:'part A')
        part.setId(1)
        parts.add part
        thing.parts = parts

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        "true" == xml.parts.'@array'.text()
    }

    def "Test that associated map carries the map attribute"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        MarshalledThingContributor contrib = new MarshalledThingContributor( firstName:'John', lastName:'Smith' )
        contrib.id = 5
        thing.contributors=['smith':contrib]


        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        "true" == xml.contributors.'@map'.text()
    }

    def "Test that non-association collection carries the array attribute"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        "true" == xml.simpleArray.'@array'.text()
    }

    def "Test that null non-association map marshalls as empty node"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.simpleMap = null

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        "true" == xml.simpleMap.'@map'.text()
        0      == xml.simpleMap.children().size()
    }

    def "Test that empty non-association map marshalls as empty node"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.simpleMap = [:]

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        "true" == xml.simpleMap.'@map'.text()
        0      == xml.simpleMap.children().size()
    }


    def "Test that null non-association collection marshalls as empty node"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.simpleArray = null

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        "true" == xml.simpleArray.'@array'.text()
        0      == xml.simpleArray.children().size()
    }

    def "Test that empt non-association collection marshalls as empty node"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.simpleArray = []

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        "true" == xml.simpleArray.'@array'.text()
        0      == xml.simpleArray.children().size()
    }


    def "Test special processing of association field"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        boolean invoked = false
        marshaller.metaClass.processField << {
            BeanWrapper beanWrapper, GrailsDomainClassProperty property, XML xml ->
            invoked = true
            if (property.getName() == 'parts') {
                xml.startNode('theParts')
                beanWrapper.getPropertyValue(property.getName()).each {
                    xml.startNode('part')

                    xml.startNode('theId')
                    xml.convertAnother(it.getId())
                    xml.end()
                    xml.startNode('theCode')
                    xml.convertAnother(it.code)
                    xml.end()

                    xml.end()
                }
                xml.end()
                return false
            } else {
                return true
            }
        }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        def parts = []
        def part = new MarshalledPartOfThing(code:'partA',desc:'part A')
        part.setId(1)
        parts.add part
        thing.parts = parts


        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0 == xml.parts.size()
        true    == invoked
        1       == xml.theParts.size()
        1       == xml.theParts.children().size()
        1       == xml.theParts.part[0].theId.size()
        'partA' == xml.theParts.part[0].theCode.text()
    }

    def "Test Collection based association field marshalled as short object"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        def parts = []
        def part = new MarshalledPartOfThing(code:'partA',desc:'part A')
        part.setId(1)
        parts.add part
        part = new MarshalledPartOfThing(code:'partB',desc:'part B')
        part.setId(2)
        parts.add part
        thing.parts = parts

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        '/marshalled-part-of-things/1' == xml.parts.shortObject[0]._link.text()
        '/marshalled-part-of-things/2' == xml.parts.shortObject[1]._link.text()
        0 == xml.parts.code.size()
        0 == xml.parts.description.size()
    }

    def "Test Map based association field marshalled as short object"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        MarshalledThingContributor contrib = new MarshalledThingContributor( firstName:'John', lastName:'Smith' )
        contrib.id = 5
        thing.contributors.put('smith',contrib)
        contrib = new MarshalledThingContributor( firstName:'John', lastName:'Anderson' )
        contrib.id = 6
        thing.contributors.put('anderson',contrib)

        when:
        def content = render( thing )
        def xml = XML.parse content
        def smith = xml.contributors.children().find { it.@key.text() == 'smith' }
        def anderson = xml.contributors.children().find { it.@key.text() == 'anderson' }

        then:
        2                                  == xml.contributors.children().size()
        'smith'                            == xml.contributors.entry[0].@key.text()
        '/marshalled-thing-contributors/5' == smith.shortObject._link.text()
        '/marshalled-thing-contributors/6' == anderson.shortObject._link.text()
        1                                  == xml.contributors.entry[0].children().size()
        1                                  == xml.contributors.entry[1].children().size()
    }

    def "Test many-to-one association field marshalled as short object"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        MarshalledOwnerOfThing owner = new MarshalledOwnerOfThing( firstName:'John', lastName:'Smith' )
        owner.id = 5
        thing.owner = owner


        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0 == xml.owner.firstName.size()
        '/marshalled-owner-of-things/5' == xml.owner.shortObject._link.text()
    }

    def "Test one-to-one association field marshalled as short object"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        MarshalledSubPartOfThing part = new MarshalledSubPartOfThing( code:'zz' )
        part.id = 5
        thing.subPart = part


        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0 == xml.subPart.code.size()
        '/marshalled-sub-part-of-things/5' == xml.subPart.shortObject._link.text()
    }

    def "Test embedded association field marshalled as full object"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        MarshalledThingEmbeddedPart part = new MarshalledThingEmbeddedPart( serialNumber:'ad34fa', description:'foo' )
        thing.embeddedPart = part

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        'ad34fa' == xml.embeddedPart.serialNumber.text()
        'foo'    == xml.embeddedPart.description.text()
    }

    def "Test additional fields"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.processAdditionalFields << {BeanWrapper wrapper, XML xml ->
            xml.startNode('additionalProp')
            xml.convertAnother(wrapper.getPropertyValue('code') + ':' + wrapper.getPropertyValue('description'))
            xml.end()
        }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'aa thing' )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        'AA:aa thing' == xml.additionalProp.text()
    }

    private void register( String name, def marshaller ) {
        XML.createNamedConfig( "BasicDomainClassMarshaller:" + testName + ":$name" ) { json ->
            json.registerObjectMarshaller( marshaller, 100 )
        }
    }

    private void register( def marshaller ) {
        register( "default", marshaller )
    }

    private String render( String name, def obj ) {
        XML.use( "BasicDomainClassMarshaller:" + testName + ":$name" ) {
            return (obj as XML) as String
        }
    }

    private String render( def obj ) {
        render( "default", obj )
    }
}