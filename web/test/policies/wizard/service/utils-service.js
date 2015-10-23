describe('policies.wizard.service.utils-service', function () {
  beforeEach(module('webApp'));

  var service = null;

  beforeEach(inject(function (UtilsService) {
    service = UtilsService;

  }));

  describe("should be able to find an element in an JSON array telling him what attribute it has to used to search the element", function () {

    it("if any param is invalid, returns -1", function () {
      var position = service.findElementInJSONArray([], {}, null);
      expect(position).toBe(-1);
      result = service.findElementInJSONArray([], null, "any_attribute");
      expect(position).toBe(-1);
      position = service.findElementInJSONArray(null, {}, "any_attribute");
      expect(position).toBe(-1);
    });

    it("if array is empty, returns -1", function () {
      expect(service.findElementInJSONArray([], {"any_attribute": "fake"}, "any_attribute")).toBe(-1);
    });

    describe("if array is not empty", function () {
      var array = [{"name": "fakename 1"}, {"name": "fakename 2"}, {"name": "fakename 3"}];

      it("if array does not contain the element, returns -1", function () {
        var fakeJson = {"name": "fake value"};

        expect(service.findElementInJSONArray(array, fakeJson, "name")).toBe(-1);
      });

      it("if array contains the element, returns -1", function () {
        var position = 0;
        var fakeJson = {"name": array[position].name};

        expect(service.findElementInJSONArray(array, fakeJson, "name")).toBe(position);

        position = 1;
        fakeJson = {"name": array[position].name};

        expect(service.findElementInJSONArray(array, fakeJson, "name")).toBe(position);
      });

    });

    describe("should be able to remove from an array all elements whose position is contained in the array of positions passed as param", function () {
      it("if array of positions is empty, no element is removed from array", function () {
        var array = [{"any": "fake value"}];
        var positions = [];

        var result = service.removeItemsFromArray(array, positions);
        expect(result).toEqual(array);
      });

      it("if array of positions is not empty, elements whose position is included in the position array are removed from array", function () {
        var array = [{"any": "fake value 1"}, {"any": "fake value 2"}, {"any": "fake value 3"}, {"any": "fake value 4"}];
        var positions = [1, 3];
        var expectedArray = [{"any": "fake value 1"}, {"any": "fake value 3"}];

        var result = service.removeItemsFromArray(array, positions);
        expect(result).toEqual(expectedArray);
      });

      it("should work correctly although the position array is introduced not sorted", function () {
        var array = [{"any": "fake value 1"}, {"any": "fake value 2"}, {"any": "fake value 3"}, {"any": "fake value 4"}];
        var positions = [3, 2];
        var expectedArray = [{"any": "fake value 1"}, {"any": "fake value 2"}];

        var result = service.removeItemsFromArray(array, positions);
        expect(result).toEqual(expectedArray);
      });

    });

    describe("should be able to return a string introduced with an increment number at the end", function () {
      it("if string does not contain a number, it is returned with (2) at the end", function () {
        var string = "fake name";

        expect(service.autoIncrementName(string)).toBe(string + "(2)");
      });

      it("if string contains a number, it is returned with the number plus one at the end", function () {
        var currentNumber = 5;
        var text = "fake name";
        var string = text + "("+ currentNumber + ")";
        expect(service.autoIncrementName(string)).toBe(text + "(" +(currentNumber + 1) + ")");
      });

    });

    describe("should be able to return an array of names from a JSON array with more attributes", function () {
      it("if the introduced array is null, undefined or is empty, it returns an empty array", function () {
        expect(service.getItemNames()).toEqual([]);
        expect(service.getItemNames(null)).toEqual([]);
        expect(service.getItemNames(undefined)).toEqual([]);
        expect(service.getItemNames([])).toEqual([]);
        expect(service.getItemNames(undefined)).toEqual([]);
      });

      it("if the introduced array is valid, it returns an array with names of the element contained in the introduced array with attribute 'name'", function () {
        var fakeJson = {"name": "fake json 1"};
        var invalidJson = {"no_exist_name": "invalid json"};
        var fakeJson2 = {"name": "fake json 2"};

        var array = [fakeJson, invalidJson, fakeJson2];
        expect(service.getItemNames(array).length).toEqual(2);
        expect(service.getItemNames(array)[0]).toEqual(fakeJson.name);
        expect(service.getItemNames(array)[1]).toEqual(fakeJson2.name);
      });

    });

    describe("should be able to return a JSON array of names from an array of json with more attributes", function () {
      it("if the introduced array is null, undefined or is empty, it returns an empty array", function () {
        expect(service.getNamesJSONArray()).toEqual([]);
        expect(service.getNamesJSONArray(null)).toEqual([]);
        expect(service.getNamesJSONArray(undefined)).toEqual([]);
        expect(service.getNamesJSONArray([])).toEqual([]);
        expect(service.getNamesJSONArray(undefined)).toEqual([]);
      });

      it("if the introduced array is valid, it returns a JSON array of names of the elements contained in the introduced array with attribute 'name'", function () {
        var fakeJson = {"name": "fake json 1"};
        var invalidJson = {"no_exist_name": "invalid json"};
        var fakeJson2 = {"name": "fake json 2"};

        var array = [fakeJson, invalidJson, fakeJson2];
        expect(service.getNamesJSONArray(array).length).toEqual(2);
        expect(service.getNamesJSONArray(array)[0]).toEqual(fakeJson);
        expect(service.getNamesJSONArray(array)[1]).toEqual(fakeJson2);
      });

    });
  })
});
