package com.test.myspring

import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.query.Param
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/user")
class UserController(
    private val repository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager,
) {

    init {
        repository.save(
            User(
                username = "thinhtrai1",
                password = passwordEncoder.encode("1"),
                email = "ducthinhtrai@gmail.com",
                firstname = "Thinh",
                lastname = "Duck",
                role = "USER",
            ),
        )
    }

    @PostMapping("/register")
    fun register(
        @Param("username") username: String,
        @Param("password") password: String,
        @Param("email") email: String,
        @Param("firstname") firstname: String,
        @Param("lastname") lastname: String,
    ): ResponseEntity<*> {
        if (repository.existsByUsername(username)) {
            error("Username is already taken!")
        }
        if (repository.existsByEmail(email)) {
            error("Email is already taken!")
        }
        val user = repository.save(User(username, passwordEncoder.encode(password), email, firstname, lastname, "USER"))
        return UserResponse(
            user.id!!,
            username,
            email,
            firstname,
            lastname,
            generateAuthentication(username)
        ).ok()
    }

    @PostMapping("/login")
    fun login(@Param("username") username: String, @Param("password") password: String): ResponseEntity<*> {
        val auth = authenticationManager.authenticate(UsernamePasswordAuthenticationToken(username, password))
        SecurityContextHolder.getContext().authentication = auth
        val user = (auth.principal as CustomUserDetail).user
        return UserResponse(
            user.id!!,
            username,
            user.email,
            user.firstname,
            user.lastname,
            generateAuthentication(username)
        ).ok()
    }
}

@RestController
@RequestMapping("/api")
class HomeController(private val userRepository: UserRepository) {

    @GetMapping("/users")
    fun findAll() = userRepository.findAll().ok()
}

@RestController
@RequestMapping("/api/product")
class ProductController(private val repository: ProductRepository) {

    @GetMapping("")
    fun findAll(
        @RequestParam("search") search: String? = null,
        @RequestParam("id") id: Long? = null,
    ) = if (search != null) {
        repository.findAllByNameContainingIgnoreCase(search).ok()
    } else if (id != null) {
        repository.findById(id).run {
            if (isPresent) {
                get().ok()
            } else {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")
            }
        }
    } else {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing request param")
    }

    @GetMapping("/{page}")
    fun getForYou(@PathVariable page: Int) = repository.findAll(PageRequest.of(page, 10)).ok()
}

@RestController
@RequestMapping("/api/peoples")
class PeopleController(private val repository: PeopleRepository) {

    @GetMapping("")
    fun getPeoples() = repository.findAll().ok()

    @GetMapping("/{page}/{size}")
    fun getPeoples(
        @PathVariable page: Int,
        @PathVariable size: Int,
    ) = repository.findAll(PageRequest.of(page, size)).ok()
}