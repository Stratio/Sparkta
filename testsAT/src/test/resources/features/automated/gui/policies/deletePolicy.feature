@web @rest
Feature: Test deleting a policy in Sparta GUI

	Background: Setup Sparta GUI
		Given My app is running in '${SPARTA_HOST}:${SPARTA_PORT}'
		Given I send requests to '${SPARTA_HOST}:${SPARTA_API_PORT}'
			
	Scenario: Stop a policy
		# Create input fragment
		Given I send a 'POST' request to '/fragment' based on 'schemas/fragments/fragment.conf' as 'json' with:
		| id | DELETE | N/A |
		| fragmentType | UPDATE | input |
		| name | UPDATE | flumeinput |
		| element.type | UPDATE | Flume |
		Then the service response status must be '200'
		And I save element '$.id' in environment variable 'previousFragmentID'
		When I send a 'GET' request to '/fragment/input'
		Then the service response status must be '200' and its response length must be '1'
		
		# Create output fragment	
		Given I send a 'POST' request to '/fragment' based on 'schemas/fragments/fragment.conf' as 'json' with:
		| id | DELETE | N/A |
		| fragmentType | UPDATE | output |
		| name | UPDATE | printoutput |
		| element.type | UPDATE | Print |
		Then the service response status must be '200'
		And I save element '$.id' in environment variable 'previousFragmentID_2'
		When I send a 'GET' request to '/fragment/output'
		Then the service response status must be '200' and its response length must be '1'
		
		# Create policy using these fragments
		When I send a 'POST' request to '/policy' based on 'schemas/policies/policy.conf' as 'json' with:
		| fragments[0].id | UPDATE | !{previousFragmentID} |
		| fragments[0].name | UPDATE | myInputFragment |
		| fragments[0].fragmentType | UPDATE | input |
		| fragments[1].id | UPDATE | !{previousFragmentID_2} |
		| fragments[1].name | UPDATE | myOutputFragment |
		| fragments[1].fragmentType | UPDATE | output |
		| id | DELETE | N/A |
		| input | DELETE | N/A |
		| outputs | DELETE | N/A |
		| name | UPDATE | myPolicy |
		Then the service response status must be '200'
		And I save element '$.id' in environment variable 'previousPolicyID'
		# Check list of policies
		When I send a 'GET' request to '/policy/all'	
		Then the service response status must be '200' and its response length must be '1'		
	
		# Browse to policies
		Given I browse to '/#/dashboard/policies'
		Then I wait '2' seconds
		And '1' element exists with 'css:i[data-qa^="policy-context-menu-"]'
		And '1' element exists with 'css:i[data-qa="policy-context-menu-!{previousPolicyID}"]'
		
		# Press menu
		Given I click on the element on index '0'
		Then '1' element exists with 'css:st-menu-element[data-qa="policy-context-menu-!{previousPolicyID}-delete"]'
		
		# Delete policy
		Given I click on the element on index '0'
		Then '1' element exists with 'css:aside[data-qa="delete-policy-modal"]'
		Given '1' element exists with 'css:button[data-qa="modal-ok-button"]'
		When I click on the element on index '0'
		Then I wait '2' seconds
		And '0' elements exist with 'css:i[data-qa^="policy-context-menu-"]'
		# Check that policyContext has disappeared
		When I send a 'GET' request to '/policyContext'
		Then the service response status must be '200'
		
		Scenario: Delete fragments
		When I send a 'DELETE' request to '/fragment/input/!{previousFragmentID}'
		Then the service response status must be '200'
		When I send a 'GET' request to '/fragment/input'
		Then the service response status must be '200' and its response must contain the text '[]'
		When I send a 'DELETE' request to '/fragment/output/!{previousFragmentID_2}'
		Then the service response status must be '200'
		When I send a 'GET' request to '/fragment/output'
		Then the service response status must be '200' and its response must contain the text '[]'
		
		
