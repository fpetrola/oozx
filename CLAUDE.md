Tratar siempre de achicar el codigo no de agregar lineas porque si. La idea es si hay que agregar funcionalidad buscar primero si existe algo parecido y reusarlo o encontrar abstracciones que puede cumplir con lo que hay y lo nuevo.
El codigo tiene que tender a achicarse por tener mejor implementacion cada vez.
Los comentarios en el codigo hay que tratar de evitarlos, solo tiene que estar presente cuando el nombre de la clase, metodo o variable no refleja claramente lo que hace, o en caso de que sea un algoritmo complejo que necesite explicacion.

Antes de crear una clase o archivo nuevo, decir en una linea en que clase existente iria ese codigo y por que no va ahi. Si no hay una razon fuerte, va en la clase existente.

Si estoy por escribir un comentario que explica por que duplico o evito codigo que ya existe, ese comentario es la señal de que la decision esta mal: parar, reportar el problema del codigo existente y preguntar.

Si algo existente no se puede usar porque esta roto (excepcion, dependencia faltante, mal diseñado), arreglar eso o preguntar. Nunca escribir una segunda implementacion en paralelo.

Cada concepto tiene un solo dueño. La logica que depende del formato de archivo (tap/tzx/csw) va solamente en Tape: ningun switch ni if por formato fuera de ahi. Lo mismo para el resto de los conceptos del emulador.

El codigo nuevo va donde pertenece el concepto, no en el paquete donde esta el que lo llama.

Al terminar un cambio, decir el neto de lineas y que se reuso.
