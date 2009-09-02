import text.regexp.RegexpBackend

use text/regexp/pcre

Pcre: cover from pcre*

pcre_compile: extern func (String, Int, String**, Int*, Pointer) -> Pcre
pcre_exec: extern func(Pcre, Pointer, String, Int, Int, Int, Int*, Int) -> Int
pcre_free: extern func(Pointer)


PCRE: class extends RegexpBackend {
	CASELESS : extern(PCRE_CASELESS) static const Int

	error: String
	errorNum: Int
	re: Pcre
	
	destroy: func {
		pcre_free(re)
	}
	
	setPattern: func(pattern: String) {
		this pattern = pattern
		
		re = pcre_compile(pattern, 0, error&, errorNum&, null)
		if (! re)
			printf("PCRE compilation failed at expression offset %d: %s\n", errorNum, error)
	}
	
	getName: func -> String {
		return "PCRE"
	}
	
	matches: func(haystack: String) -> Bool {
		return matches(haystack, 0)
	}
	
	matches: func~withOptions(haystack: String, options: Int) -> Bool { 
		// offsets := gc_malloc(10 * sizeof(Int)) as Int*
		return pcre_exec(re, null, haystack, haystack length(), 0, options, null, 0) >= 0
	}
	
	
}
