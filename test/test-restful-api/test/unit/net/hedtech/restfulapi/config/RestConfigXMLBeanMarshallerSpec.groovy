/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import grails.test.mixin.*
import grails.test.mixin.support.*

import net.hedtech.restfulapi.*
import net.hedtech.restfulapi.beans.*
import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.xml.*
import net.hedtech.restfulapi.marshallers.xml.*

import spock.lang.*


@TestMixin(GrailsUnitTestMixin)
class RestConfigXMLBeanMarshallerSpec extends Specification {

    def "Test xml bean marshaller in marshaller group"() {
        setup:
        def src =
        {
            marshallerGroups {
                group 'xmlBean' marshallers {
                    xmlBeanMarshaller {
                        supports SimpleBean
                        elementName 'Bean'
                        field 'foo' name 'bar'
                        includesFields {
                            field 'bar' name 'customBar'
                        }
                        excludesFields {
                            field 'foobar'
                        }
                    }
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        def marshaller = config.marshallerGroups['xmlBean'].marshallers[0].instance

        then:
        SimpleBean                                == marshaller.supportClass
        'Bean'                                    == marshaller.elementName
        ['foo':'bar','bar':'customBar']           == marshaller.fieldNames
        ['bar']                                   == marshaller.includedFields
        ['foobar']                                == marshaller.excludedFields
    }

    def "Test xml bean marshaller template parsing"() {
        setup:
        def src =
        {
            xmlBeanMarshallerTemplates {
                template 'one' config {
                }

                template 'two' config {
                    inherits = ['one']
                    priority = 5
                    supports SimpleBean
                    elementName 'Bean'
                    requiresIncludedFields true
                    field 'foo' name 'bar'
                    field 'f1'
                    field 'f2'
                    includesFields {
                        field 'foo' name 'foobar'
                    }
                    excludesFields {
                        field 'bar'
                    }
                    additionalFields {->}
                    additionalFieldsMap = ['a':'b','c':'d']
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def mConfig = config.xmlBean.configs['two']

        then:
        2                 == config.xmlBean.configs.size()
        ['one']           == mConfig.inherits
        5                 == mConfig.priority
        SimpleBean        == mConfig.supportClass
        'Bean'            == mConfig.elementName
        true              == mConfig.requireIncludedFields
        ['foo':'foobar']  == mConfig.fieldNames
        ['foo']           == mConfig.includedFields
        true              == mConfig.useIncludedFields
        ['bar']           == mConfig.excludedFields
        1                 == mConfig.additionalFieldClosures.size()
        ['a':'b','c':'d'] == mConfig.additionalFieldsMap
    }

    def "Test xml bean marshaller creation"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/xml']
                    marshallers {
                        xmlBeanMarshaller {
                            supports SimpleBean
                            elementName 'Bean'
                            requiresIncludedFields true
                            field 'owner' name 'myOwner'
                            includesFields {
                                field 'code' name 'productCode'
                                field 'parts'
                            }
                            excludesFields {
                                field 'description'
                            }
                            additionalFields {Map m ->}
                            additionalFieldsMap = ['foo':'bar']
                        }
                    }
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def marshaller = config.getRepresentation( 'things', 'application/xml' ).marshallers[0].instance

        then:
        SimpleBean                               == marshaller.supportClass
        'Bean'                                   == marshaller.elementName
        true                                     == marshaller.requireIncludedFields
        ['owner':'myOwner','code':'productCode'] == marshaller.fieldNames
        ['code','parts']                         == marshaller.includedFields
        ['description']                          == marshaller.excludedFields
        1                                        == marshaller.additionalFieldClosures.size()
        ['foo':'bar']                            == marshaller.additionalFieldsMap
    }

    def "Test xml bean marshaller creation from merged configuration"() {
        setup:
        def src =
        {
            xmlBeanMarshallerTemplates {
                template 'one' config {
                    includesFields {
                        field 'field1'
                    }
                }

                template 'two' config {
                    includesFields {
                        field 'field2'
                    }
                }
            }

            resource 'things' config {
                representation {
                    mediaTypes = ['application/xml']
                    marshallers {
                        xmlBeanMarshaller {
                            inherits = ['one','two']
                            supports Thing
                            includesFields {
                                field 'code'
                                field 'description'
                            }
                        }
                    }
                    extractor = 'extractor'
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def marshaller = config.getRepresentation( 'things', 'application/xml' ).marshallers[0].instance

        then:
        ['field1','field2','code','description'] == marshaller.includedFields
    }

}