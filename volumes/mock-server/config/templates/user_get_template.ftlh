{
	"template" : "get user template",
	"api" : "${api}",
	"operation" : "${method}",
	"uuid" : "${uuid}",
	"users" : {
		<#assign cnt = users?size>
		<#list users[0..cnt-2] as user>
			{
				"firstName": "${user.firstName}"
				"lastName": "${user.lastName}"
				"age": "${user.age}"
			},
		</#list>
		{
			"firstName": "${users[cnt-1].firstName}"
			"lastName": "${users[cnt-1].lastName}"
			"age": "${users[cnt-1].age}"
		}
	}
}