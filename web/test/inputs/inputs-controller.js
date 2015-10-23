describe('com.stratio.sparkta.inputs.inputs.controller', function () {
	beforeEach(module('webApp'));
	beforeEach(module('served/input.json'));
  beforeEach(module('served/inputList.json'));
  beforeEach(module('served/inputTemplate.json'));
  beforeEach(module('served/policyList.json'));

	var ctrl, scope, filter, modalMock, fakeNewInputTemplate, fakeInput = null;

	beforeEach(inject(function ($controller, $q, $rootScope, $httpBackend, $filter, _servedInputList_, _servedInputTemplate_, _servedInput_, _servedPolicyList_){
		/*scope = $rootScope.$new();*/
		scope = $rootScope;
		fakeInputList = _servedInputList_;
		fakeNewInputTemplate = _servedInputTemplate_;
		fakeInput = _servedInput_;
		fakePolicyList = _servedPolicyList_;

    resolvedInputListPromise = function () {
      var defer = $q.defer();
      defer.resolve(fakeInputList);

      return defer.promise;
    };

    rejectedInputListPromise = function () {
      var defer = $q.defer();
      defer.reject({"data":{"i18nCode": "111"}});

      return defer.promise;
    };

    resolvedNewInputTemplate = function () {
      var defer = $q.defer();
      defer.resolve(fakeNewInputTemplate);

      return defer.promise;
    };

    resolvedNewInput = function () {
    	var defer = $q.defer();
			defer.resolve(fakeInput);

			return {"result": defer.promise};
    };

    resolvedEditInput = function () {
    	var defer = $q.defer();
			defer.resolve({"index":1, "data": fakeInput});

			return {"result": defer.promise};
    };

    resolvedDeleteInput = function () {
    	var defer = $q.defer();
			defer.resolve({"index":2});

			return {"result": defer.promise};
    };

    resolverPolicyList = function() {
    	var defer = $q.defer();
    	defer.resolve(fakePolicyList);

    	return defer.promise;
    };

    $httpBackend.when('GET', 'languages/en-US.json').respond({});

		fragmentFactoryMock = jasmine.createSpyObj('FragmentFactory', ['getFragments']);
		fragmentFactoryMock.getFragments.and.callFake(resolvedInputListPromise);

		utilsServiceMock = jasmine.createSpyObj('UtilsService', ['getItemNames', 'autoIncrementName']);

		modalMock = jasmine.createSpyObj('$modal', ['open']);

		templateFactoryMock = jasmine.createSpyObj('TemplateFactory', ['getNewFragmentTemplate']);
		templateFactoryMock.getNewFragmentTemplate.and.callFake(resolvedNewInputTemplate);

		policyFactoryMock = jasmine.createSpyObj('PolicyFactory', ['getPolicyByFragmentId']);
		policyFactoryMock.getPolicyByFragmentId.and.callFake(resolverPolicyList);

		filter = $filter;

		ctrl = $controller ('InputsCtrl', {
			'FragmentFactory': fragmentFactoryMock,
			'$filter': filter,
			'$modal': modalMock,
			'UtilsService': utilsServiceMock,
			'TemplateFactory': templateFactoryMock,
			'PolicyFactory': policyFactoryMock
		});

		scope.$digest();
	}));

	describe('Initialize controller', function() {
		var newFakeInputList = null;

		beforeEach(function() {
			newFakeInputList = angular.copy(fakeInputList);
		});

		it('Should call getInputs function and return a list of Inputs', inject(function ($controller) {
			fragmentFactoryMock.getFragments.and.callFake(resolvedInputListPromise);
			ctrl = $controller ('InputsCtrl', {
				'FragmentFactory': fragmentFactoryMock,
				'$filter': filter,
				'$modal': modalMock,
				'UtilsService': utilsServiceMock,
				'TemplateFactory': templateFactoryMock,
				'PolicyFactory': policyFactoryMock
			});

			scope.$digest();

			expect(ctrl.error).toBeFalsy();
			expect(ctrl.inputsData).toEqual(newFakeInputList);
		}));

		it('Should call getInputs function and get an error', inject(function ($controller) {
			fragmentFactoryMock.getFragments.and.callFake(rejectedInputListPromise);
			ctrl = $controller ('InputsCtrl', {
				'FragmentFactory': fragmentFactoryMock,
				'$filter': filter,
				'$modal': modalMock,
				'UtilsService': utilsServiceMock,
				'TemplateFactory': templateFactoryMock,
				'PolicyFactory': policyFactoryMock
			});

			scope.$digest();

			expect(ctrl.error).toBeTruthy();
			expect(ctrl.errorMessage).toBe("_INPUT_ERROR_111_");
		}));

		it('Should call getInputTypes function and create an array of the amount of each input types', function() {
			expect(ctrl.inputTypes).toEqual([{"type":"Socket","count": 2},{"type":"Flume","count": 2},{"type":"Kafka","count": 2}]);
		});
	});


	describe('Create a new input', function() {
		var newFakeInputList, fakeListOfNames = null;

		beforeEach(function() {
			modalMock.open.and.callFake(resolvedNewInput);
			ctrl.inputsData = angular.copy(fakeInputList);
			fakeListOfNames = ['test_input_kafka','test_input_socket'];
		});

		it('Should call createInput function and show new input modal', function() {
			utilsServiceMock.getItemNames.and.returnValue(fakeListOfNames);

	    var fakeCreateInputData = {
	      'fragmentType': 'input',
	      'fragmentNamesList' : fakeListOfNames,
	      'texts': {
	        'title': '_INPUT_WINDOW_NEW_TITLE_',
	        'button': '_INPUT_WINDOW_NEW_BUTTON_',
	        'button_icon': 'icon-circle-plus'
	      }
	    };

			ctrl.createInput();

	    var params = modalMock.open.calls.mostRecent().args[0];
			expect(params.resolve.item()).toEqual(fakeCreateInputData);

			params.resolve.fragmentTemplates();
			expect(templateFactoryMock.getNewFragmentTemplate).toHaveBeenCalledWith(fakeCreateInputData.fragmentType);
		});

		it('Should return OK when closing the create modal', function() {
			ctrl.createInput();
			scope.$digest();

			expect(ctrl.inputsData[6]).toBe(fakeInput);
			expect(ctrl.inputTypes).toEqual([{"type":"Socket","count": 2},{"type":"Flume","count": 2},{"type":"Kafka","count": 2},{"type":"RabbitMQ","count": 1}]);
		});
	});

	describe('Edit an input', function() {
		var fakeIndex, fakeInputName, fakeInputId, fakeInputSelected = null;

		beforeEach(function () {
			modalMock.open.and.callFake(resolvedEditInput);
			ctrl.inputsData = angular.copy(fakeInputList);
			fakeIndex = 1;
 			fakeInputName = ctrl.inputsData[fakeIndex].name;
			fakeInputId = ctrl.inputsData[fakeIndex].id;
			fakeInputSelected = ctrl.inputsData[fakeIndex];
 		});

		it('Shuold call editInput function and show an edit input modal', function() {
			var fakeListOfNames = ['test_input_kafka','test_input_socket'];
			utilsServiceMock.getItemNames.and.returnValue(fakeListOfNames);

			var fakeEditInputData = {
        'originalName': fakeInputName,
        'fragmentType': 'input',
        'index': fakeIndex,
        'fragmentSelected': fakeInputSelected,
        'fragmentNamesList' : fakeListOfNames,
        'texts': {
            'title': '_INPUT_WINDOW_MODIFY_TITLE_',
            'button': '_INPUT_WINDOW_MODIFY_BUTTON_',
            'button_icon': 'icon-circle-check',
            'secondaryText2': '_INPUT_WINDOW_EDIT_MESSAGE2_',
            'policyRunningMain': '_INPUT_CANNOT_BE_DELETED_',
            'policyRunningSecondary': '_INTPUT_WINDOW_POLICY_RUNNING_MESSAGE_',
            'policyRunningSecondary2': '_INTPUT_WINDOW_POLICY_RUNNING_MESSAGE2_'
        }
	    };

	    ctrl.editInput(fakeInputName, fakeInputId, fakeIndex);

	    var params = modalMock.open.calls.mostRecent().args[0];
	    expect(params.resolve.item()).toEqual(fakeEditInputData);

	    params.resolve.fragmentTemplates();
	    expect(templateFactoryMock.getNewFragmentTemplate).toHaveBeenCalledWith(fakeEditInputData.fragmentSelected.fragmentType);

	    params.resolve.policiesAffected();
			expect(policyFactoryMock.getPolicyByFragmentId).toHaveBeenCalledWith(fakeEditInputData.fragmentSelected.fragmentType, fakeEditInputData.fragmentSelected.id);
		});

		it('Should return OK when closing the edit modal', function() {
			ctrl.editInput(fakeInputName, fakeInputId, fakeIndex);
			scope.$digest();

			expect(ctrl.inputsData[fakeIndex]).toEqual(fakeInput);
			expect(ctrl.inputTypes).toEqual([{"type":"Socket","count": 2},{"type":"RabbitMQ","count": 1},{"type":"Kafka","count": 2},{"type":"Flume","count": 1}]);
		});
	});

	describe('Delete an input', function() {
		var fragmentType, fragmentId, fragmentId = null;

		beforeEach(function() {
			modalMock.open.and.callFake(resolvedDeleteInput);
			ctrl.inputsData = angular.copy(fakeInputList);

			fakeFragmentType = 'input';
			fakeFragmentId = '2581f20a-fd83-4315-be45-1923c5sEFfl';
			fakeIndex = 2;
		});

		it('Should call deleteInput function and show a delete input modal', function() {
			var fakeInputToDelete = {
        'type': fakeFragmentType,
        'id': fakeFragmentId,
        'index': fakeIndex,
        'texts': {
          'title': '_INPUT_WINDOW_DELETE_TITLE_',
          'mainText': '_ARE_YOU_COMPLETELY_SURE_',
          'secondaryText1': '_INPUT_WINDOW_DELETE_MESSAGE_',
          'secondaryText2': '_INPUT_WINDOW_DELETE_MESSAGE2_',
          'policyRunningMain': '_INPUT_CANNOT_BE_DELETED_',
          'policyRunningSecondary': '_INTPUT_WINDOW_POLICY_RUNNING_MESSAGE_',
          'policyRunningSecondary2': '_INTPUT_WINDOW_DELETE_POLICY_RUNNING_MESSAGE2_'
        }
      };

			ctrl.deleteInput(fakeFragmentType, fakeFragmentId, fakeIndex);

			var params = modalMock.open.calls.mostRecent().args[0]
	    expect(params.resolve.item()).toEqual(fakeInputToDelete);

	    params.resolve.policiesAffected();
			expect(policyFactoryMock.getPolicyByFragmentId).toHaveBeenCalledWith(fakeInputToDelete.type, fakeInputToDelete.id);
		});

		it('Should return OK when closing the delete modal', function() {
			ctrl.deleteInput(fakeFragmentType, fakeFragmentId, fakeIndex);
			scope.$digest();
			expect(ctrl.inputsData[fakeIndex]).toEqual(fakeInputList[fakeIndex+1]);
		});
	});
/*
	describe('Duplicate an input', function() {
		var fakeInputId, fakeNewName, fakeListOfNames, fakeInputSelected, fakeInputListIndex = null;

		beforeEach(function() {
			fakeInputListIndex = 2;
			fakeInputId = fakeInputList[fakeInputListIndex].id;
			fakeNewName = 'test_input_kafka(1)';
			fakeListOfNames = ['test_input_kafka','test_input_socket'];
			fakeInputSelected = fakeInputList[fakeInputListIndex];
		});

		it('Should call duplicateInput function and show a duplicate input modal', function() {
			utilsServiceMock.autoIncrementName.and.returnValue(fakeNewName);
			utilsServiceMock.getItemNames.and.returnValue(fakeListOfNames);

      var fakeDuplicateInputData = {
        'fragmentData': fakeInputSelected,
        'fragmentNamesList': fakeListOfNames,
        'texts': {
          'title': '_INPUT_WINDOW_DUPLICATE_TITLE_'
        }
      };

			ctrl.duplicateInput(fakeInputId);

			var params = modalMock.open.calls.mostRecent().args[0];
	    expect(params.resolve.item()).toEqual(fakeDuplicateInputData);

	    scope.$digest();

	    expect(ctrl.inputsData[6]).toBe(fakeInputSelected);
		});

		it('Should return OK when closing the duplicate modal', function() {
			ctrl.duplicateInput(fakeInputId);
			scope.$digest();

			expect(ctrl.inputsData[6]).toBe(fakeInputSelected);
			expect(ctrl.inputTypes).toEqual([{"type":"Socket","count": 1},{"type":"Flume","count": 3},{"type":"Kafka","count": 2},{"type":"RabbitMQ","count": 1}]);
		});
	});
*/
});
