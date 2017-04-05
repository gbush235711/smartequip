# smartequip
A coding sample for SmartEquip

This is a simple example of a challenge/response web service for verifying a human actor.  (The actual challenge as given isn't complex enough to dissuade a bot, but in principle it could be more complex.)

The current version of code uses a symmetric cryptographic key known only to the server in order to optimize for a truly stateless web service.

Upon POSTing an empty form to the URL /, the client will receive a JSON response of the form:

		{"key":"SomeRandomizedKeyHereThatGoesInTheResponse","challenge":"Please sum the numbers 6, 10, 3"}

The client may then perform the calculation and GET the URL of the form

		/?key=SomeRandomizedKeyHereThatGoesInTheResponse&challenge=Please+sum+the+numbers+6,+10,+3&response=19

where *key* and *challenge* are the same as were provided from the server and *response* is the answer the client calculated.

If the response is correct for the given key and challenge, then the server will respond with a HTTP 200 status.  Otherwise, it will fail with an HTTP 400 status.

The idea behind the POST/GET format is to use REST-like "resource based" semantics, where a POST signals a resource creation and a GET queries the status of a resource.  The nature of the challenge string makes resource semantics a bit awkward here, and the problem is constrained to have all operations mapped to /, but a possible alternate interface would be to have the POST forward to a URL like /RandomizedKey, where the question is visible and then have the GET on a resource like /RandomizedKey/Answer.  This would feel a bit more resource-y to me, but the idea would basically be the same.

This particular implementation is optimized to satisfy the "Safeguard against cheating" and "Stateless" optional requirements.

When the client requests a challenge, the server will generate one randomly and return it along with an encrypted payload (128-bit AES) containing the challenge, the expected answer, a timestamp when the challenge was generated and a random "salt" value.

When the client gives his response, the server will decrypt the payload and compare the given challenge and response to its contents, as well as check the response was received within 5 minutes of the challenge.  The salt is discarded.  If all passes, then the server will report success.

This clearly stores no state on the server, but it is also difficult for a client to provide a reponse unless he is actually answering a given challenge.  This is because 1) the client does not know the private key that is used to encrypt the payload and 2) the embedded salt and timestamp counter replay attacks where, for example, a dictionary of all known challenge keys is kept and reused later.

I wouldn't use this demo to actually try to detect bots, but some of the same principles could be potentially be used in a real application.
