/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import grails.test.mixin.*
import spock.lang.*

import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.xml.*
import net.hedtech.restfulapi.*
import grails.test.mixin.support.*
import net.hedtech.restfulapi.marshallers.xml.*

@TestMixin(GrailsUnitTestMixin)
class XMLDomainMarshallerConfigSpec extends Specification {

    def "Test inherits"() {
        setup:
        def src = {
            inherits = ['one','two']
        }

        when:
        def config = invoke( src )

        then:
        ['one','two'] == config.inherits

    }

    def "Test priority"() {
        setup:
        def src = {
            priority = 50
        }

        when:
        def config = invoke( src )

        then:
        50 == config.priority
    }

    def "Test supportClass"() {
        setup:
        def src = {
            supports String
        }

        when:
        def config = invoke( src )

        then:
        String == config.supportClass
        true   == config.isSupportClassSet
    }

    def "Test substitutions for field names"() {
        setup:
        def src = {
            field 'one' name 'modOne'
            field 'two' name 'modTwo'
            includesFields {
                field 'three' name 'modThree'
                field 'four' name 'modFour'
            }
        }

        when:
        def config = invoke( src )

        then:
        ['one':'modOne','two':'modTwo','three':'modThree','four':'modFour'] == config.fieldNames
    }

    def "Test included fields"() {
        setup:
        def src = {
            includesFields {
                field 'one'
                field 'two'
            }
        }

        when:
        def config = invoke( src )

        then:
        ['one','two'] == config.includedFields
    }

    def "Test requires included fields"() {
        setup:
        def src = {
            requiresIncludedFields true
        }

        when:
        def config = invoke( src )

        then:
        true == config.requireIncludedFields
    }


    def "Test excluded fields"() {
        setup:
        def src = {
            excludesFields {
                field 'one'
                field 'two'
            }
        }

        when:
        def config = invoke( src )

        then:
        ['one','two'] == config.excludedFields
    }


    def "Test exclude id and version"() {
       setup:
        def src = {
            includesId false
            includesVersion false
        }

        when:
        def config = invoke( src )

        then:
        false == config.includeId
        false == config.includeVersion
    }

    def "Test additional field closures"() {
        setup:
        def storage = []
        def src = {
            additionalFields {
                Map m -> storage.add 'one'
            }
            additionalFields {
                Map m -> storage.add 'two'
            }
        }

        when:
        def config = invoke( src )
        config.additionalFieldClosures.each {
            it.call([:])
        }

        then:
        2             == config.additionalFieldClosures.size()
        ['one','two'] == storage
    }

    def "Test additionalFieldsMap"() {
        setup:
        def src = {
            additionalFieldsMap = ['one':'one','two':'two']
        }

        when:
        def config = invoke( src )

        then:
        [one:'one',two:'two'] == config.additionalFieldsMap
    }

    def "Test field resource names"() {
        setup:
        def src = {
            field 'owner' resource 'thing-owners'
            field 'manager' name 'mgr' resource 'thing-managers'
            field 'accountant' resource 'thing-accountants' name 'acct'
        }

        when:
        def config = invoke(src)

        then:
        ['owner':'thing-owners','manager':'thing-managers','accountant':'thing-accountants'] == config.fieldResourceNames
    }

    def "Test field resource names in includes"() {
        setup:
        def src = {
            includesFields {
                field 'owner' resource 'thing-owners'
                field 'manager' name 'mgr' resource 'thing-managers'
                field 'accountant' resource 'thing-accountants' name 'acct'
            }
        }

        when:
        def config = invoke(src)

        then:
        ['owner':'thing-owners','manager':'thing-managers','accountant':'thing-accountants'] == config.fieldResourceNames
    }

    def "Test custom short object closure"() {
        setup:
        def invoked = false
        def src = {
            shortObject { Map m ->
                invoked = true
            }
        }

        when:
        def config = invoke(src)
        config.shortObjectClosure.call([:])

        then:
        true == invoked
        true == config.isShortObjectClosureSet
    }

    def "Test merging domain marshaller configurations"() {
        setup:
        def c1 = { Map m -> }
        def c2 = { Map m -> }
        XMLDomainMarshallerConfig one = new XMLDomainMarshallerConfig(
            supportClass:Thing,
            fieldNames:['foo':'foo1','bar':'bar1'],
            includedFields:['foo','bar'],
            excludedFields:['e1','e2'],
            additionalFieldClosures:[{app,bean,xml ->}],
            additionalFieldsMap:['one':'one','two':'two'],
            fieldResourceNames:['f1':'r1','f2':'r2'],
            shortObjectClosure:c1,
            includeId:true,
            includeVersion:true,
            requireIncludedFields:true
        )
        XMLDomainMarshallerConfig two = new XMLDomainMarshallerConfig(
            supportClass:PartOfThing,
            fieldNames:['foo':'foo2','baz':'baz1'],
            includedFields:['baz'],
            excludedFields:['e3'],
            additionalFieldClosures:[{app,bean,xml ->}],
            additionalFieldsMap:['two':'2','three':'3'],
            fieldResourceNames:['f2':'name3','f3':'r3'],
            shortObjectClosure:c2,
            includeId:false,
            includeVersion:false,
            requireIncludedFields:false
        )

        when:
        def config = one.merge(two)

        then:
        true                                     == one.isSupportClassSet
        true                                     == two.isSupportClassSet
        PartOfThing                              == two.supportClass
        ['foo':'foo2','bar':'bar1','baz':'baz1'] == config.fieldNames
        ['foo','bar','baz']                      == config.includedFields
        ['e1','e2','e3']                         == config.excludedFields
        2                                        == config.additionalFieldClosures.size()
        ['one':'one',"two":'2','three':'3']      == config.additionalFieldsMap
        ['f1':'r1','f2':'name3','f3':'r3']       == config.fieldResourceNames
        c2                                       == config.shortObjectClosure
        false                                    == config.includeId
        false                                    == config.includeVersion
        false                                    == config.requireIncludedFields
    }

    def "Test merging domain marshaller configurations does not alter either object"() {
        setup:
        def c1 = { Map m -> }
        def c2 = { Map m -> }
        XMLDomainMarshallerConfig one = new XMLDomainMarshallerConfig(
            supportClass:Thing,
            fieldNames:['foo':'foo1','bar':'bar1'],
            includedFields:['foo','bar'],
            excludedFields:['e1','e2'],
            additionalFieldClosures:[{app,bean,xml ->}],
            additionalFieldsMap:['one':'1'],
            fieldResourceNames:['f1':'r1','f2':'r2'],
            shortObjectClosure:c1,
            includeId:true,
            includeVersion:true,
            requireIncludedFields:true
        )
        XMLDomainMarshallerConfig two = new XMLDomainMarshallerConfig(
            supportClass:PartOfThing,
            fieldNames:['foo':'foo2','baz':'baz1'],
            includedFields:['baz'],
            excludedFields:['e3'],
            additionalFieldClosures:[{app,bean,xml ->}],
            additionalFieldsMap:['two':'2'],
            fieldResourceNames:['f2':'name3','f3':'r3'],
            shortObjectClosure:c2,
            includeId:false,
            includeVersion:false,
            requireIncludedFields:false
        )

        when:
        one.merge(two)

        then:
        true                        == one.isSupportClassSet
        ['foo':'foo1','bar':'bar1'] == one.fieldNames
        ['foo','bar']               == one.includedFields
        ['e1','e2']                 == one.excludedFields
        1                           == one.additionalFieldClosures.size()
        ['one':'1']                 == one.additionalFieldsMap
        ['f1':'r1','f2':'r2']       == one.fieldResourceNames
        c1                          == one.shortObjectClosure
        true                        == one.includeId
        true                        == one.includeVersion
        true                        == one.requireIncludedFields

        true                        == two.isSupportClassSet
        ['foo':'foo2','baz':'baz1'] == two.fieldNames
        ['baz']                     == two.includedFields
        ['e3']                      == two.excludedFields
        1                           == two.additionalFieldClosures.size()
        ['two':'2']                 == two.additionalFieldsMap
        ['f2':'name3','f3':'r3']    == two.fieldResourceNames
        c2                          == two.shortObjectClosure
        false                       == two.includeId
        false                       == two.includeVersion
        false                       == two.requireIncludedFields
    }

    def "Test merging domain marshaller with support class set only on the left"() {
        setup:
        XMLDomainMarshallerConfig one = new XMLDomainMarshallerConfig(
            supportClass:Thing
        )
        XMLDomainMarshallerConfig two = new XMLDomainMarshallerConfig(
        )

        when:
        def config = one.merge(two)

        then:
        Thing == config.supportClass
        true  == config.isSupportClassSet
    }

    def "Test merging domain marshaller with short object closure set only on the left"() {
        setup:
        def c1 = { Map m -> }
        XMLDomainMarshallerConfig one = new XMLDomainMarshallerConfig(
            shortObjectClosure:c1
        )
        XMLDomainMarshallerConfig two = new XMLDomainMarshallerConfig(
        )

        when:
        def config = one.merge(two)

        then:
        c1   == config.shortObjectClosure
        true == config.isShortObjectClosureSet
    }

    def "Test merging domain marshaller with require included fields set only on the left"() {
        setup:
        def c1 = { Map m -> }
        XMLDomainMarshallerConfig one = new XMLDomainMarshallerConfig(
            requireIncludedFields:true
        )
        XMLDomainMarshallerConfig two = new XMLDomainMarshallerConfig(
        )

        when:
        def config = one.merge(two)

        then:
        true == config.requireIncludedFields
    }


    def "Test resolution of domain marshaller configuration inherits"() {
        setup:
        XMLDomainMarshallerConfig part1 = new XMLDomainMarshallerConfig(
        )
        XMLDomainMarshallerConfig part2 = new XMLDomainMarshallerConfig(
        )
        XMLDomainMarshallerConfig part3 = new XMLDomainMarshallerConfig(
        )
        XMLDomainMarshallerConfig combined = new XMLDomainMarshallerConfig(
            inherits:['part1','part2']
        )
        XMLDomainMarshallerConfig actual = new XMLDomainMarshallerConfig(
            inherits:['combined','part3']
        )
        ConfigGroup group = new ConfigGroup()
        group.configs = ['part1':part1,'part2':part2,'part3':part3,'combined':combined]

        when:
        def resolvedList = group.resolveInherited( actual )

        then:
        [part1,part2,combined,part3,actual] == resolvedList
    }

    def "Test merge order of domain marshaller configuration inherits"() {
        setup:
        XMLDomainMarshallerConfig part1 = new XMLDomainMarshallerConfig(
            fieldNames:['1':'part1','2':'part1','3':'part1']
        )
        XMLDomainMarshallerConfig part2 = new XMLDomainMarshallerConfig(
            fieldNames:['2':'part2','3':'part2']

        )
        XMLDomainMarshallerConfig actual = new XMLDomainMarshallerConfig(
            inherits:['part1','part2'],
            fieldNames:['3':'actual']
        )
        ConfigGroup group = new ConfigGroup()
        group.configs = ['part1':part1,'part2':part2]

        when:
        def config = group.getMergedConfig( actual )

        then:
        ['1':'part1','2':'part2','3':'actual'] == config.fieldNames
    }

    def "Test repeated field clears previous settings"() {
        setup:
        def src = {
            field 'one' name 'modOne' resource 'resource-ones'
            field 'one'
            field 'two' name 'modTwo' resource 'resource-twos'
            includesFields {
                field 'two'
            }
        }

        when:
        def config = invoke( src )

        then:
        [:] == config.fieldNames
        [:] == config.fieldResourceNames

    }

    private XMLDomainMarshallerConfig invoke( Closure c ) {
        XMLDomainMarshallerDelegate delegate = new XMLDomainMarshallerDelegate()
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        delegate.config
    }
}